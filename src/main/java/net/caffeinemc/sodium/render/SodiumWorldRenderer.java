package net.caffeinemc.sodium.render;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.interop.vanilla.math.frustum.Frustum;
import net.caffeinemc.sodium.interop.vanilla.mixin.WorldRendererHolder;
import net.caffeinemc.sodium.render.chunk.RenderSectionManager;
import net.caffeinemc.sodium.render.chunk.draw.ChunkCameraContext;
import net.caffeinemc.sodium.render.chunk.draw.ChunkRenderMatrices;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPassManager;
import net.caffeinemc.sodium.render.chunk.state.ChunkRenderData;
import net.caffeinemc.sodium.render.terrain.context.ImmediateTerrainRenderCache;
import net.caffeinemc.sodium.util.ListUtil;
import net.caffeinemc.sodium.util.NativeBuffer;
import net.caffeinemc.sodium.world.ChunkTracker;
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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;

import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;

/**
 * Provides an extension to vanilla's {@link WorldRenderer}.
 */
public class SodiumWorldRenderer {
    private final MinecraftClient client;

    private ClientWorld world;
    private int renderDistance;

    private double lastCameraX, lastCameraY, lastCameraZ;
    private double lastCameraPitch, lastCameraYaw;
    private float lastFogDistance;

    private boolean useEntityCulling;

    private RenderSectionManager renderSectionManager;
    private ChunkRenderPassManager renderPassManager;
    private ChunkTracker chunkTracker;

    /**
     * @return The SodiumWorldRenderer based on the current dimension
     */
    public static SodiumWorldRenderer instance() {
        var instance = instanceNullable();

        if (instance == null) {
            throw new IllegalStateException("No renderer attached to active world");
        }

        return instance;
    }

    /**
     * @return The SodiumWorldRenderer based on the current dimension, or null if none is attached
     */
    public static SodiumWorldRenderer instanceNullable() {
        var world = MinecraftClient.getInstance().worldRenderer;

        if (world instanceof WorldRendererHolder) {
            return ((WorldRendererHolder) world).getSodiumWorldRenderer();
        }

        return null;
    }

    public SodiumWorldRenderer(MinecraftClient client) {
        this.client = client;
    }

    public void setWorld(ClientWorld world) {
        // Check that the world is actually changing
        if (this.world == world) {
            return;
        }

        // If we have a world is already loaded, unload the renderer
        if (this.world != null) {
            this.unloadWorld();
        }

        // If we're loading a new world, load the renderer
        if (world != null) {
            this.loadWorld(world);
        }
    }

    private void loadWorld(ClientWorld world) {
        this.world = world;
        this.chunkTracker = new ChunkTracker();

        ImmediateTerrainRenderCache.createRenderContext(this.world);

        this.initRenderer();
    }

    private void unloadWorld() {
        ImmediateTerrainRenderCache.destroyRenderContext(this.world);

        if (this.renderSectionManager != null) {
            this.renderSectionManager.destroy();
            this.renderSectionManager = null;
        }

        this.chunkTracker = null;
        this.world = null;
    }

    /**
     * @return The number of chunk renders which are visible in the current camera's frustum
     */
    public int getVisibleChunkCount() {
        return this.renderSectionManager.getVisibleSectionCount();
    }

    /**
     * Notifies the chunk renderer that the graph scene has changed and should be re-computed.
     */
    public void scheduleTerrainUpdate() {
        // BUG: seems to be called before init
        if (this.renderSectionManager != null) {
            this.renderSectionManager.markGraphDirty();
        }
    }

    /**
     * @return True if no chunks are pending rebuilds
     */
    public boolean isTerrainRenderComplete() {
        return this.renderSectionManager.getBuilder().isBuildQueueEmpty();
    }

    /**
     * Called prior to any chunk rendering in order to update necessary state.
     */
    public void updateChunks(Camera camera, Frustum frustum, @Deprecated(forRemoval = true) int frame, boolean spectator) {
        NativeBuffer.reclaim(false);

        this.useEntityCulling = SodiumClientMod.options().performance.useEntityCulling;

        if (this.client.options.getViewDistance() != this.renderDistance) {
            this.reload();
        }

        Profiler profiler = this.client.getProfiler();
        profiler.push("camera_setup");

        ClientPlayerEntity player = this.client.player;

        if (player == null) {
            throw new IllegalStateException("Client instance has no active player entity");
        }

        Vec3d pos = camera.getPos();
        float pitch = camera.getPitch();
        float yaw = camera.getYaw();
        float fogDistance = RenderSystem.getShaderFogEnd();

        boolean dirty = pos.x != this.lastCameraX || pos.y != this.lastCameraY || pos.z != this.lastCameraZ ||
                pitch != this.lastCameraPitch || yaw != this.lastCameraYaw || fogDistance != this.lastFogDistance;

        if (dirty) {
            this.renderSectionManager.markGraphDirty();
        }

        this.lastCameraX = pos.x;
        this.lastCameraY = pos.y;
        this.lastCameraZ = pos.z;
        this.lastCameraPitch = pitch;
        this.lastCameraYaw = yaw;
        this.lastFogDistance = fogDistance;

        profiler.swap("chunk_update");

        this.chunkTracker.update();

        this.renderSectionManager.setFrameIndex(frame);
        this.renderSectionManager.updateChunks();

        if (this.renderSectionManager.isGraphDirty()) {
            profiler.swap("chunk_graph_rebuild");

            this.renderSectionManager.update(new ChunkCameraContext(camera), frustum, spectator);
        }

        profiler.swap("visible_chunk_tick");

        this.renderSectionManager.tickVisibleRenders();

        profiler.pop();

        Entity.setRenderDistanceMultiplier(MathHelper.clamp((double) this.client.options.getViewDistance() / 8.0D, 1.0D, 2.5D) * (double) this.client.options.entityDistanceScaling);
    }

    /**
     * Performs a render pass for the given {@link RenderLayer} and draws all visible chunks for it.
     */
    public void drawChunkLayer(RenderLayer renderLayer, MatrixStack matrixStack) {
        ChunkRenderPass renderPass = this.renderPassManager.getRenderPassForLayer(renderLayer);
        this.renderSectionManager.renderLayer(ChunkRenderMatrices.from(matrixStack), renderPass);
    }

    public void reload() {
        if (this.world == null) {
            return;
        }

        this.initRenderer();
    }

    private void initRenderer() {
        if (this.renderSectionManager != null) {
            this.renderSectionManager.destroy();
            this.renderSectionManager = null;
        }

        this.renderDistance = this.client.options.getViewDistance();

        this.renderPassManager = ChunkRenderPassManager.createDefaultMappings();

        this.renderSectionManager = new RenderSectionManager(SodiumClientMod.DEVICE, this, this.renderPassManager, this.world, this.renderDistance);
        this.renderSectionManager.reloadChunks(this.chunkTracker);
    }

    public void renderTileEntities(MatrixStack matrices, BufferBuilderStorage bufferBuilders, Long2ObjectMap<SortedSet<BlockBreakingInfo>> blockBreakingProgressions,
                                   Camera camera, float tickDelta) {
        VertexConsumerProvider.Immediate immediate = bufferBuilders.getEntityVertexConsumers();

        Vec3d cameraPos = camera.getPos();
        double x = cameraPos.getX();
        double y = cameraPos.getY();
        double z = cameraPos.getZ();

        BlockEntityRenderDispatcher blockEntityRenderer = MinecraftClient.getInstance().getBlockEntityRenderDispatcher();

        for (BlockEntity blockEntity : this.renderSectionManager.getVisibleBlockEntities()) {
            BlockPos pos = blockEntity.getPos();

            matrices.push();
            matrices.translate((double) pos.getX() - x, (double) pos.getY() - y, (double) pos.getZ() - z);

            VertexConsumerProvider consumer = immediate;
            SortedSet<BlockBreakingInfo> breakingInfos = blockBreakingProgressions.get(pos.asLong());

            if (breakingInfos != null && !breakingInfos.isEmpty()) {
                int stage = breakingInfos.last().getStage();

                if (stage >= 0) {
                    MatrixStack.Entry entry = matrices.peek();
                    VertexConsumer transformer = new OverlayVertexConsumer(bufferBuilders.getEffectVertexConsumers().getBuffer(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(stage)), entry.getPositionMatrix(), entry.getNormalMatrix());
                    consumer = (layer) -> layer.hasCrumbling() ? VertexConsumers.union(transformer, immediate.getBuffer(layer)) : immediate.getBuffer(layer);
                }
            }


            blockEntityRenderer.render(blockEntity, tickDelta, matrices, consumer);

            matrices.pop();
        }

        for (BlockEntity blockEntity : this.renderSectionManager.getGlobalBlockEntities()) {
            BlockPos pos = blockEntity.getPos();

            matrices.push();
            matrices.translate((double) pos.getX() - x, (double) pos.getY() - y, (double) pos.getZ() - z);

            blockEntityRenderer.render(blockEntity, tickDelta, matrices, immediate);

            matrices.pop();
        }
    }

    public void onChunkAdded(int x, int z) {
        if (this.chunkTracker.loadChunk(x, z)) {
            this.renderSectionManager.onChunkAdded(x, z);
        }
    }

    public void onChunkLightAdded(int x, int z) {
        this.chunkTracker.onLightDataAdded(x, z);
    }

    public void onChunkRemoved(int x, int z) {
        if (this.chunkTracker.unloadChunk(x, z)) {
            this.renderSectionManager.onChunkRemoved(x, z);
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

        // Entities outside the valid world height will never map to a rendered chunk
        // Always render these entities or they'll be culled incorrectly!
        if (box.maxY < 0.5D || box.minY > 255.5D) {
            return true;
        }

        // Ensure entities with outlines or nametags are always visible
        if (this.client.hasOutline(entity) || entity.shouldRenderName()) {
            return true;
        }

        return this.isBoxVisible(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    public boolean doesChunkHaveFlag(int x, int z, int status) {
        return this.chunkTracker.hasMergedFlags(x, z, status);
    }

    public boolean isBoxVisible(double x1, double y1, double z1, double x2, double y2, double z2) {
        int minX = MathHelper.floor(x1 - 0.5D) >> 4;
        int minY = MathHelper.floor(y1 - 0.5D) >> 4;
        int minZ = MathHelper.floor(z1 - 0.5D) >> 4;

        int maxX = MathHelper.floor(x2 + 0.5D) >> 4;
        int maxY = MathHelper.floor(y2 + 0.5D) >> 4;
        int maxZ = MathHelper.floor(z2 + 0.5D) >> 4;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    if (this.renderSectionManager.isSectionVisible(x, y, z)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public String getChunksDebugString() {
        // C: visible/total
        // TODO: add dirty and queued counts
        return String.format("C: %s/%s", this.renderSectionManager.getVisibleSectionCount(), this.renderSectionManager.getTotalSections());
    }

    /**
     * Schedules chunk rebuilds for all chunks in the specified block region.
     */
    public void scheduleRebuildForBlockArea(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean important) {
        this.scheduleRebuildForChunks(minX >> 4, minY >> 4, minZ >> 4, maxX >> 4, maxY >> 4, maxZ >> 4, important);
    }

    /**
     * Schedules chunk rebuilds for all chunks in the specified chunk region.
     */
    public void scheduleRebuildForChunks(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean important) {
        for (int chunkX = minX; chunkX <= maxX; chunkX++) {
            for (int chunkY = minY; chunkY <= maxY; chunkY++) {
                for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
                    this.scheduleRebuildForChunk(chunkX, chunkY, chunkZ, important);
                }
            }
        }
    }

    /**
     * Schedules a chunk rebuild for the render belonging to the given chunk section position.
     */
    public void scheduleRebuildForChunk(int x, int y, int z, boolean important) {
        this.renderSectionManager.scheduleRebuild(x, y, z, important);
    }

    public Collection<String> getMemoryDebugStrings() {
        return this.renderSectionManager.getDebugStrings();
    }

    public ChunkTracker getChunkTracker() {
        return this.chunkTracker;
    }
}
