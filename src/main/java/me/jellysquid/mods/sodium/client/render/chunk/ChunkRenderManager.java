package me.jellysquid.mods.sodium.client.render.chunk;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkRender;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkRenderUploadTask;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ColumnRender;
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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ChunkRenderManager<T extends ChunkRenderData> {
    private final MinecraftClient client;

    private final ObjectList<ChunkRender<T>> chunksToRebuild = new ObjectArrayList<>();
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
    }

    public void setWorld(ClientWorld world) {
        this.world = world;

        this.isRenderGraphDirty = true;

        this.renderDistance = this.client.options.viewDistance;

        if (world == null) {
            this.chunksToRebuild.clear();

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

            ((ChunkManagerWithStatusListener) world.getChunkManager()).setListener(this.chunkGraph);
        }
    }

    public int getCompletedChunkCount() {
        return this.chunkGraph.getDrawableChunks().size();
    }

    public void scheduleTerrainUpdate() {
        this.isRenderGraphDirty = true;
    }

    public void updateChunks(long limitTime) {
        this.isRenderGraphDirty |= this.chunkBuilder.upload();
        this.isRenderGraphDirty |= this.chunkGraph.cleanup();

        int limit = this.chunkBuilder.getBudget();
        int uploaded = 0;

        if (!this.chunksToRebuild.isEmpty()) {
            Iterator<ChunkRender<T>> iterator = this.chunksToRebuild.iterator();

            while (uploaded < limit && iterator.hasNext()) {
                ChunkRender<T> chunk = iterator.next();

                if (chunk.needsImportantRebuild()) {
                    chunk.rebuildImmediately();
                } else {
                    chunk.rebuild();
                }

                iterator.remove();

                ++uploaded;
            }
        }
    }

    public boolean isTerrainRenderComplete() {
        return this.chunksToRebuild.isEmpty() && this.chunkBuilder.isEmpty();
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

        this.isRenderGraphDirty = this.isRenderGraphDirty || !this.chunksToRebuild.isEmpty() ||
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

        this.performRebuilds(blockPos);

        this.client.getProfiler().pop();
    }

    private void performRebuilds(BlockPos blockPos) {
        List<CompletableFuture<ChunkRenderUploadTask>> futures = new ArrayList<>();

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
                    futures.add(render.rebuildImmediately());
                } else {
                    render.rebuild();
                }

                this.isRenderGraphDirty = true;
            }
        }

        // TODO: perform an upload on the main-thread when any chunk is completed to reduce idle
        for (CompletableFuture<ChunkRenderUploadTask> future : futures) {
            ChunkRenderUploadTask task = future.join();

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

        this.chunkRenderer.begin();

        RenderSystem.pushMatrix();

        while (true) {
            if (notTranslucent) {
                if (!it.hasNext()) {
                    break;
                }
            } else if (!it.hasPrevious()) {
                break;
            }

            this.chunkRenderer.render(notTranslucent ? it.next() : it.previous(), renderLayer, matrixStack, x, y, z);
        }

        this.chunkRenderer.end();

        RenderSystem.popMatrix();

        RenderSystem.clearCurrentColor();

        profiler.pop();

        renderLayer.endDrawing();
    }

    public void renderChunkDebugInfo(Camera camera) {
        // TODO: re-implement
    }

    public void clearRenderers() {
        this.chunksToRebuild.clear();
    }

    public void reload() {
        if (this.world == null) {
            return;
        }

        this.isRenderGraphDirty = true;
        this.renderDistance = this.client.options.viewDistance;

        if (this.chunkGraph != null) {
            this.chunkGraph.reset();
        }

        this.chunkBuilder.reset();
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
}
