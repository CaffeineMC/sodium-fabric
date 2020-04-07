package me.jellysquid.mods.sodium.client.render.chunk;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderState;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.tasks.ChunkRenderUploadTask;
import me.jellysquid.mods.sodium.client.world.ChunkManagerWithStatusListener;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListener;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ChunkRenderManager<T extends ChunkRenderState> implements ChunkStatusListener {
    private final MinecraftClient client;
    private final ChunkRenderBackend<T> chunkRenderer;

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
    private Set<BlockEntity> globalBlockEntities = new ObjectOpenHashSet<>();

    public ChunkRenderManager(MinecraftClient client, ChunkRenderBackend<T> chunkRenderer) {
        this.client = client;
        this.chunkRenderer = chunkRenderer;
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
                this.chunkBuilder.stopWorkers();
            }
        } else {
            if (this.chunkBuilder == null) {
                this.chunkBuilder = new ChunkBuilder();
            }

            this.chunkBuilder.setWorld(this.world);
            this.chunkBuilder.startWorkers();

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

        Entity.setRenderDistanceMultiplier(MathHelper.clamp((double) this.client.options.viewDistance / 8.0D, 1.0D, 2.5D));

        this.client.getProfiler().swap("rebuildNear");

        this.updateChunks(blockPos);

        this.client.getProfiler().pop();
    }

    private void updateChunks(BlockPos blockPos) {
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

        // Try to complete some other work on the main thread while we wait for rebuilds to complete
        this.isRenderGraphDirty |= this.chunkBuilder.upload();
        this.isRenderGraphDirty |= this.chunkGraph.cleanup();

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
        return new ChunkRender<>(this, this.chunkBuilder, this.chunkRenderer.createRenderState(), column, x, y, z);
    }

    public void scheduleRebuildForBlock(int x, int y, int z) {
        ChunkRender<T> node = this.chunkGraph.getRender(x, y, z);

        if (node != null) {
            node.scheduleRebuild(true);
        }
    }

    public void renderTileEntities(MatrixStack matrices, BufferBuilderStorage bufferBuilders, Long2ObjectMap<SortedSet<BlockBreakingInfo>> blockBreakingProgressions,
                                   Camera camera, float tickDelta) {
        VertexConsumerProvider.Immediate immediate = bufferBuilders.getEntityVertexConsumers();

        Vec3d cameraPos = camera.getPos();
        double x = cameraPos.getX();
        double y = cameraPos.getY();
        double z = cameraPos.getZ();

        for (BlockEntity blockEntity : this.chunkGraph.getVisibleBlockEntities()) {
            BlockPos pos = blockEntity.getPos();

            matrices.push();
            matrices.translate((double) pos.getX() - x, (double) pos.getY() - y, (double) pos.getZ() - z);

            VertexConsumerProvider consumer = immediate;
            SortedSet<BlockBreakingInfo> breakingInfos = blockBreakingProgressions.get(pos.asLong());

            if (breakingInfos != null && !breakingInfos.isEmpty()) {
                int stage = breakingInfos.last().getStage();

                if (stage >= 0) {
                    VertexConsumer transformer = new TransformingVertexConsumer(this.bufferBuilders.getEffectVertexConsumers().getBuffer(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(stage)), matrices.peek());
                    consumer = (layer) -> layer.method_23037() ? VertexConsumers.dual(transformer, immediate.getBuffer(layer)) : immediate.getBuffer(layer);
                }
            }

            BlockEntityRenderDispatcher.INSTANCE.render(blockEntity, tickDelta, matrices, consumer);

            matrices.pop();
        }

        for (BlockEntity blockEntity : this.globalBlockEntities) {
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

    public void onChunkRenderUpdated(ChunkMeshInfo meshBefore, ChunkMeshInfo meshAfter) {
        Collection<BlockEntity> entitiesBefore = meshBefore.getGlobalBlockEntities();

        if (!entitiesBefore.isEmpty()) {
            this.globalBlockEntities.removeAll(entitiesBefore);
        }

        Collection<BlockEntity> entitiesAfter = meshAfter.getGlobalBlockEntities();

        if (!entitiesAfter.isEmpty()) {
            this.globalBlockEntities.addAll(entitiesAfter);
        }
    }
}
