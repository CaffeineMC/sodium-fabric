package me.jellysquid.mods.sodium.client.render.chunk;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkRender;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkRenderUploadTask;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ColumnRender;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListener;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;

public class ChunkRenderManager<T extends ChunkRenderData> implements ChunkStatusListener {
    private final MinecraftClient client;

    private final ObjectList<BlockEntity> visibleBlockEntities = new ObjectArrayList<>();
    private final ChunkRenderer<T> chunkRenderer;

    private ClientWorld world;

    private int renderDistance;

    private ChunkBuilder chunkBuilder;

    private double lastTranslucentSortX;
    private double lastTranslucentSortY;
    private double lastTranslucentSortZ;

    private double lastCameraX;
    private double lastCameraY;
    private double lastCameraZ;

    private double lastCameraPitch;
    private double lastCameraYaw;

    private boolean isRenderGraphDirty;

    private ChunkGraph<T> chunkGraph;
    private BufferBuilderStorage bufferBuilders;

    public ChunkRenderManager(MinecraftClient client, ChunkRenderer<T> chunkRenderer) {
        this.client = client;
        this.chunkRenderer = chunkRenderer;

        Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 2);
    }

    public void setWorld(ClientWorld world) {
        this.world = world;

        this.isRenderGraphDirty = true;

        this.renderDistance = this.client.options.viewDistance;

        if (world == null) {
            if (this.chunkGraph != null) {
                this.chunkGraph.reset();
                this.chunkGraph = null;
            }

            if (this.chunkBuilder != null) {
                this.chunkBuilder.shutdown();
            }
        } else {
            if (this.chunkBuilder == null) {
                this.chunkBuilder = new ChunkBuilder();
            }

            this.chunkBuilder.setWorld(this.world);
            this.chunkBuilder.start();

            this.bufferBuilders = MinecraftClient.getInstance().getBufferBuilders();
            this.chunkGraph = new ChunkGraph<>(this, this.world, this.renderDistance);

            ((ChunkManagerWithStatusListener) world.getChunkManager()).setListener(this);
        }
    }

    public int getCompletedChunkCount() {
        return this.chunkGraph.getDrawableChunks().size();
    }

    public void scheduleTerrainUpdate() {
        this.isRenderGraphDirty = true;
    }

    public boolean isTerrainRenderComplete() {
        return this.chunkBuilder.isEmpty();
    }

    public void update(Camera camera, Frustum frustum, boolean hasForcedFrustum, int frame, boolean spectator) {
        Vec3d cameraPos = camera.getPos();

        if (this.client.options.viewDistance != this.renderDistance) {
            this.reload();
        }

        this.world.getProfiler().push("camera");

        ClientPlayerEntity player = this.client.player;

        if (player == null) {
            throw new IllegalStateException("Client instance has no active player entity");
        }

        this.chunkBuilder.setCameraPosition(cameraPos.x, cameraPos.y, cameraPos.z);

        this.world.getProfiler().swap("cull");
        this.client.getProfiler().swap("culling");

        float pitch = camera.getPitch();
        float yaw = camera.getYaw();

        this.isRenderGraphDirty = this.isRenderGraphDirty ||
                cameraPos.x != this.lastCameraX || cameraPos.y != this.lastCameraY || cameraPos.z != this.lastCameraZ ||
                pitch != this.lastCameraPitch || yaw != this.lastCameraYaw;

        this.lastCameraX = cameraPos.x;
        this.lastCameraY = cameraPos.y;
        this.lastCameraZ = cameraPos.z;
        this.lastCameraPitch = pitch;
        this.lastCameraYaw = yaw;

        this.client.getProfiler().swap("update");

        BlockPos blockPos = camera.getBlockPos();

        if (!hasForcedFrustum && this.isRenderGraphDirty) {
            this.isRenderGraphDirty = false;

            this.client.getProfiler().push("iteration");

            this.chunkGraph.calculateVisible(camera, cameraPos, blockPos, frame, frustum, spectator);

            this.client.getProfiler().pop();
        }

        Entity.setRenderDistanceMultiplier(MathHelper.clamp((double) client.options.viewDistance / 8.0D, 1.0D, 2.5D));

        this.client.getProfiler().swap("rebuildNear");

        this.updateChunks(blockPos);

        this.client.getProfiler().pop();
    }

    private void updateChunks(BlockPos blockPos) {
        List<ChunkRender<T>> immediateTasks = new ArrayList<>();

        int budget = this.chunkBuilder.getBudget();

        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (ChunkRender<T> render : this.chunkGraph.getVisibleChunks()) {
            if (!render.needsRebuild()) {
                continue;
            }

            BlockPos origin = render.getOrigin();
            pos.set(origin.getX() + 8, origin.getY() + 8, origin.getZ() + 8);

            boolean important = render.needsImportantRebuild() && pos.getSquaredDistance(blockPos) < 768.0D;

            if (important || budget-- > 0) {
                if (important) {
                    immediateTasks.add(render);
                } else {
                    render.rebuild();
                }

                this.isRenderGraphDirty = true;
            }
        }

        // Try to complete some other work on the main thread while we wait for rebuilds to complete
        this.isRenderGraphDirty |= this.chunkBuilder.upload();
        this.isRenderGraphDirty |= this.chunkGraph.cleanup();

        if (!immediateTasks.isEmpty()) {
            this.rebuildImmediately(immediateTasks);
        }
    }

    private void rebuildImmediately(List<ChunkRender<T>> chunks) {
        LinkedBlockingDeque<ChunkRenderUploadTask> finished = new LinkedBlockingDeque<>(chunks.size());

        for (ChunkRender<T> chunk : chunks) {
            CompletableFuture<ChunkRenderUploadTask> future = chunk.rebuildImmediately();
            future.thenAccept(finished::add);
        }

        int remaining = chunks.size();

        while (remaining-- > 0) {
            ChunkRenderUploadTask task = null;

            try {
                task = finished.take();
            } catch (InterruptedException ignored) { }

            if (task != null) {
                task.performUpload();
            }
        }
    }

    public void renderLayer(RenderLayer renderLayer, MatrixStack matrixStack, double x, double y, double z) {
        Profiler profiler = this.client.getProfiler();

        renderLayer.startDrawing();

        // TODO: resort transparent
//        if (renderLayer == RenderLayer.getTranslucent()) {
//            profiler.push("translucent_sort");
//
//            double g = d - this.lastTranslucentSortX;
//            double h = e - this.lastTranslucentSortY;
//            double i = f - this.lastTranslucentSortZ;
//
//            if (g * g + h * h + i * i > 1.0D) {
//                this.lastTranslucentSortX = d;
//                this.lastTranslucentSortY = e;
//                this.lastTranslucentSortZ = f;
//
//                int j = 0;
//
//                for (ChunkGraphNode<T> chunkInfo : this.chunkGraph.getVisibleChunks()) {
//                    if (j < 15 && chunkInfo.chunk.scheduleSort(renderLayer, this.chunkBuilder)) {
//                        ++j;
//                    }
//                }
//            }
//
//            profiler.pop();
//        }

        profiler.push("filterempty");
        profiler.swap(() -> "render_" + renderLayer);

        boolean notTranslucent = renderLayer != RenderLayer.getTranslucent();

        ObjectList<ChunkRender<T>> list = this.chunkGraph.getDrawableChunks();
        ObjectListIterator<ChunkRender<T>> it = list.listIterator(notTranslucent ? 0 : list.size());

        this.chunkRenderer.begin(matrixStack);

        boolean needManualTicking = SodiumClientMod.options().performance.animateOnlyVisibleTextures;

        while (true) {
            if (notTranslucent) {
                if (!it.hasNext()) {
                    break;
                }
            } else if (!it.hasPrevious()) {
                break;
            }

            ChunkRender<T> render = notTranslucent ? it.next() : it.previous();

            if (needManualTicking) {
                render.tickTextures();
            }

            this.chunkRenderer.render(render, renderLayer, matrixStack, x, y, z);
        }

        this.chunkRenderer.end(matrixStack);

        RenderSystem.clearCurrentColor();

        profiler.pop();

        renderLayer.endDrawing();
    }

    public void renderChunkDebugInfo(Camera camera) {
        // TODO: re-implement
    }

    public void reload() {
        if (this.world == null) {
            return;
        }

        this.isRenderGraphDirty = true;
        this.renderDistance = this.client.options.viewDistance;

        if (this.chunkGraph != null) {
            this.chunkGraph.reset();
            this.chunkGraph.setRenderDistance(this.renderDistance);
        }

        this.chunkBuilder.setWorld(this.world);
    }

    public ChunkRender<T> createChunkRender(ColumnRender<T> column, int x, int y, int z) {
        return new ChunkRender<>(this.chunkBuilder, this.chunkRenderer.createRenderData(), column, x, y, z);
    }

    public void scheduleRebuildForBlock(int x, int y, int z, boolean important) {
        ChunkRender<T> node = this.chunkGraph.getRender(x, y, z);

        if (node != null) {
            node.scheduleRebuild(true);
        }
    }

    public void renderTileEntities(MatrixStack matrices, BufferBuilderStorage bufferBuilders, Camera camera, float tickDelta) {
        VertexConsumerProvider.Immediate immediate = bufferBuilders.getEntityVertexConsumers();

        Vec3d cameraPos = camera.getPos();
        double x = cameraPos.getX();
        double y = cameraPos.getY();
        double z = cameraPos.getZ();

        for (BlockEntity blockEntity : this.chunkGraph.getVisibleBlockEntities()) {
            BlockPos pos = blockEntity.getPos();

            matrices.push();
            matrices.translate((double) pos.getX() - x, (double) pos.getY() - y, (double) pos.getZ() - z);

            BlockEntityRenderDispatcher.INSTANCE.render(blockEntity, tickDelta, matrices, immediate);

            matrices.pop();
        }
    }

    @Override
    public void onChunkAdded(int x, int z) {
        this.chunkBuilder.clearCachesForChunk(x, z);
        this.chunkGraph.onChunkAdded(x, z);
    }

    @Override
    public void onChunkRemoved(int x, int z) {
        this.chunkBuilder.clearCachesForChunk(x, z);
        this.chunkGraph.onChunkRemoved(x, z);
    }
}
