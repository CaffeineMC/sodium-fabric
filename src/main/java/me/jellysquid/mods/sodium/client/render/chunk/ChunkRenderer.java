package me.jellysquid.mods.sodium.client.render.chunk;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.backends.shader.vao.ShaderVAOChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.backends.shader.vbo.ShaderVBOChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.tasks.ChunkRenderUploadTask;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPassManager;
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
import net.minecraft.util.math.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ChunkRenderer implements ChunkStatusListener {
    private static ChunkRenderer instance;

    private final MinecraftClient client;

    private ClientWorld world;
    private int renderDistance;

    private double lastTranslucentSortX;
    private double lastTranslucentSortY;
    private double lastTranslucentSortZ;

    private double lastCameraX;
    private double lastCameraY;
    private double lastCameraZ;

    private double lastCameraPitch;
    private double lastCameraYaw;

    private boolean isRenderGraphDirty;
    private boolean useEntityCulling;

    private final LongSet loadedChunkPositions = new LongOpenHashSet();
    private final Set<BlockEntity> globalBlockEntities = new ObjectOpenHashSet<>();

    private Frustum frustum;
    private ChunkRenderManager<?> chunkRenderManager;
    private ChunkBuilder chunkBuilder;
    private BlockRenderPassManager renderPassManager;
    private ChunkRenderBackend<?> chunkRenderBackend;

    public static ChunkRenderer create() {
        if (instance == null) {
            instance = new ChunkRenderer(MinecraftClient.getInstance());
        }

        return instance;
    }

    private ChunkRenderer(MinecraftClient client) {
        this.client = client;
    }

    public void setWorld(ClientWorld world) {
        this.world = world;
        this.loadedChunkPositions.clear();

        if (world == null) {
            if (this.chunkRenderManager != null) {
                this.chunkRenderManager.reset();
                this.chunkRenderManager = null;
            }

            if (this.chunkBuilder != null) {
                this.chunkBuilder.stopWorkers();
            }

            if (this.chunkRenderBackend != null) {
                this.chunkRenderBackend.delete();
                this.chunkRenderBackend = null;
            }
        } else {
            if (this.chunkBuilder == null) {
                this.chunkBuilder = new ChunkBuilder();
            }

            this.initRenderer();

            ((ChunkManagerWithStatusListener) world.getChunkManager()).setListener(this);
        }
    }

    public int getCompletedChunkCount() {
        return this.chunkRenderManager.getDrawableChunks().size();
    }

    public void scheduleTerrainUpdate() {
        this.isRenderGraphDirty = true;
    }

    public boolean isTerrainRenderComplete() {
        return this.chunkBuilder.isEmpty();
    }

    public void update(Camera camera, Frustum frustum, boolean hasForcedFrustum, int frame, boolean spectator) {
        this.frustum = frustum;

        this.applySettings();
        this.chunkRenderManager.onFrameChanged();

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

            this.chunkRenderManager.calculateVisible(camera, cameraPos, blockPos, frame, frustum, spectator);

            this.client.getProfiler().pop();
        }

        Entity.setRenderDistanceMultiplier(MathHelper.clamp((double) this.client.options.viewDistance / 8.0D, 1.0D, 2.5D));

        this.client.getProfiler().swap("rebuildNear");

        this.updateChunks(blockPos);

        this.client.getProfiler().pop();
    }

    private void applySettings() {
        this.useEntityCulling = SodiumClientMod.options().performance.useAdvancedEntityCulling;
    }

    private void updateChunks(BlockPos blockPos) {
        List<CompletableFuture<ChunkRenderUploadTask>> futures = new ArrayList<>();

        int budget = this.chunkBuilder.getBudget();

        for (ChunkRender<?> render : this.chunkRenderManager.getVisibleChunks()) {
            if (!render.needsRebuild()) {
                continue;
            }

            boolean important = render.needsImportantRebuild() && render.getSquaredDistance(blockPos) < 768.0D;

            if (important || budget-- > 0) {
                if (important) {
                    futures.add(this.chunkBuilder.createRebuildFuture(render));
                } else {
                    this.chunkBuilder.rebuild(render);
                }

                this.isRenderGraphDirty = true;
            }
        }

        // Try to complete some other work on the main thread while we wait for rebuilds to complete
        this.isRenderGraphDirty |= this.chunkBuilder.upload();
        this.isRenderGraphDirty |= this.chunkRenderManager.cleanup();

        for (CompletableFuture<ChunkRenderUploadTask> future : futures) {
            ChunkRenderUploadTask task = future.join();

            if (task != null) {
                task.performUpload();
            }
        }
    }

    public void renderLayer(RenderLayer renderLayer, MatrixStack matrixStack, double x, double y, double z) {
        BlockRenderPass blockRenderPass = this.renderPassManager.get(renderLayer);
        blockRenderPass.startDrawing();

        this.chunkRenderManager.renderLayer(matrixStack, blockRenderPass, x, y, z);

        blockRenderPass.endDrawing();

        RenderSystem.clearCurrentColor();
    }

    public void renderChunkDebugInfo(Camera camera) {
        // TODO: re-implement
    }

    public void reload() {
        if (this.world == null) {
            return;
        }

        this.initRenderer();
    }

    private void initRenderer() {
        if (this.chunkRenderManager != null) {
            this.chunkRenderManager.reset();
        }

        if (this.chunkRenderBackend != null) {
            this.chunkRenderBackend.delete();
            this.chunkRenderBackend = null;
        }

        this.isRenderGraphDirty = true;
        this.renderDistance = this.client.options.viewDistance;

        SodiumGameOptions opts = SodiumClientMod.options();

        if (opts.performance.useRenderLayerConsolidation) {
            this.renderPassManager = BlockRenderPassManager.consolidated();
        } else {
            this.renderPassManager = BlockRenderPassManager.vanilla();
        }

        if (GlVertexArray.isSupported() && opts.performance.useVertexArrays) {
            this.chunkRenderBackend = new ShaderVAOChunkRenderBackend();
        } else {
            this.chunkRenderBackend = new ShaderVBOChunkRenderBackend();
        }

        this.chunkRenderManager = new ChunkRenderManager<>(this.chunkRenderBackend, this, this.world, this.renderDistance);
        this.chunkRenderManager.addAllChunks(this.loadedChunkPositions);

        this.chunkBuilder.init(this.world, this.renderPassManager);
    }

    public void scheduleRebuild(int x, int y, int z) {
        ChunkRender<?> node = this.chunkRenderManager.getRender(x, y, z);

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

        for (BlockEntity blockEntity : this.chunkRenderManager.getVisibleBlockEntities()) {
            BlockPos pos = blockEntity.getPos();

            matrices.push();
            matrices.translate((double) pos.getX() - x, (double) pos.getY() - y, (double) pos.getZ() - z);

            VertexConsumerProvider consumer = immediate;
            SortedSet<BlockBreakingInfo> breakingInfos = blockBreakingProgressions.get(pos.asLong());

            if (breakingInfos != null && !breakingInfos.isEmpty()) {
                int stage = breakingInfos.last().getStage();

                if (stage >= 0) {
                    VertexConsumer transformer = new TransformingVertexConsumer(bufferBuilders.getEffectVertexConsumers().getBuffer(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(stage)), matrices.peek());
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
        this.loadedChunkPositions.add(ChunkPos.toLong(x, z));

        this.chunkBuilder.clearCachesForChunk(x, z);
        this.chunkRenderManager.onChunkAdded(x, z);
    }

    @Override
    public void onChunkRemoved(int x, int z) {
        this.loadedChunkPositions.remove(ChunkPos.toLong(x, z));

        this.chunkBuilder.clearCachesForChunk(x, z);
        this.chunkRenderManager.onChunkRemoved(x, z);
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

    /**
     * Returns whether or not the entity intersects with any visible chunks in the graph.
     * @return True if the entity is visible, otherwise false
     */
    public boolean isEntityVisible(Entity entity) {
        if (!this.useEntityCulling) {
            return true;
        }

        Box box = entity.getVisibilityBoundingBox();

        int minX = MathHelper.floor(box.x1 - 0.5D) >> 4;
        int minY = MathHelper.floor(box.y1 - 0.5D) >> 4;
        int minZ = MathHelper.floor(box.z1 - 0.5D) >> 4;

        int maxX = MathHelper.floor(box.x2 + 0.5D) >> 4;
        int maxY = MathHelper.floor(box.y2 + 0.5D) >> 4;
        int maxZ = MathHelper.floor(box.z2 + 0.5D) >> 4;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    if (this.chunkRenderManager.isChunkVisible(x, y, z)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static ChunkRenderer getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Renderer not initialized");
        }

        return instance;
    }

    public Frustum getFrustum() {
        return this.frustum;
    }
}
