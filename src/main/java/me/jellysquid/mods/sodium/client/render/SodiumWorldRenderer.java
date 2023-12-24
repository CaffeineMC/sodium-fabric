package me.jellysquid.mods.sodium.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.lists.SortedRenderLists;
import me.jellysquid.mods.sodium.client.render.chunk.map.ChunkTracker;
import me.jellysquid.mods.sodium.client.render.chunk.map.ChunkTrackerHolder;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.trigger.CameraMovement;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import me.jellysquid.mods.sodium.client.world.WorldRendererExtended;
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
import net.minecraft.util.profiler.Profiler;

import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;

import org.joml.Vector3d;

/**
 * Provides an extension to vanilla's {@link WorldRenderer}.
 */
public class SodiumWorldRenderer {
    private final MinecraftClient client;

    private ClientWorld world;
    private int renderDistance;

    private Vector3d lastCameraPos;
    private double lastCameraPitch, lastCameraYaw;
    private float lastFogDistance;

    private boolean useEntityCulling;

    private RenderSectionManager renderSectionManager;

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

        if (world instanceof WorldRendererExtended) {
            return ((WorldRendererExtended) world).sodium$getWorldRenderer();
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

        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            this.initRenderer(commandList);
        }
    }

    private void unloadWorld() {
        if (this.renderSectionManager != null) {
            this.renderSectionManager.destroy();
            this.renderSectionManager = null;
        }

        this.world = null;
    }

    /**
     * @return The number of chunk renders which are visible in the current camera's frustum
     */
    public int getVisibleChunkCount() {
        return this.renderSectionManager.getVisibleChunkCount();
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
    public void setupTerrain(Camera camera,
                             Viewport viewport,
                             @Deprecated(forRemoval = true) int frame,
                             boolean spectator,
                             boolean updateChunksImmediately) {
        NativeBuffer.reclaim(false);

        this.processChunkEvents();

        this.useEntityCulling = SodiumClientMod.options().performance.useEntityCulling;

        if (this.client.options.getClampedViewDistance() != this.renderDistance) {
            this.reload();
        }

        Profiler profiler = this.client.getProfiler();
        profiler.push("camera_setup");

        ClientPlayerEntity player = this.client.player;

        if (player == null) {
            throw new IllegalStateException("Client instance has no active player entity");
        }

        Vec3d posRaw = camera.getPos();
        Vector3d pos = new Vector3d(posRaw.getX(), posRaw.getY(), posRaw.getZ());
        float pitch = camera.getPitch();
        float yaw = camera.getYaw();
        float fogDistance = RenderSystem.getShaderFogEnd();

        if (this.lastCameraPos == null) {
            this.lastCameraPos = new Vector3d(pos);
        }
        boolean cameraLocationChanged = !pos.equals(this.lastCameraPos);
        boolean cameraAngleChanged = pitch != this.lastCameraPitch || yaw != this.lastCameraYaw || fogDistance != this.lastFogDistance;

        this.lastCameraPitch = pitch;
        this.lastCameraYaw = yaw;

        if (cameraLocationChanged || cameraAngleChanged) {
            this.renderSectionManager.markGraphDirty();
        }

        this.lastFogDistance = fogDistance;

        this.renderSectionManager.updateCameraState(pos, camera);

        if (cameraLocationChanged) {
            profiler.swap("translucent_triggering");

            this.renderSectionManager.processGFNIMovement(new CameraMovement(lastCameraPos, pos));
            this.lastCameraPos = new Vector3d(pos);
        }

        if (this.renderSectionManager.needsUpdate()) {
            profiler.swap("chunk_render_lists");

            this.renderSectionManager.update(camera, viewport, frame, spectator);
        }

        profiler.swap("chunk_update");

        this.renderSectionManager.cleanupAndFlip();
        this.renderSectionManager.updateChunks(updateChunksImmediately);

        profiler.swap("chunk_upload");

        this.renderSectionManager.uploadChunks();

        profiler.swap("chunk_render_tick");

        this.renderSectionManager.tickVisibleRenders();

        profiler.pop();

        Entity.setRenderDistanceMultiplier(MathHelper.clamp((double) this.client.options.getClampedViewDistance() / 8.0D, 1.0D, 2.5D) * this.client.options.getEntityDistanceScaling().getValue());
    }

    private void processChunkEvents() {
        var tracker = ChunkTrackerHolder.get(this.world);
        tracker.forEachEvent(this.renderSectionManager::onChunkAdded, this.renderSectionManager::onChunkRemoved);
    }

    /**
     * Performs a render pass for the given {@link RenderLayer} and draws all visible chunks for it.
     */
    public void drawChunkLayer(RenderLayer renderLayer, MatrixStack matrixStack, double x, double y, double z) {
        ChunkRenderMatrices matrices = ChunkRenderMatrices.from(matrixStack);

        if (renderLayer == RenderLayer.getSolid()) {
            this.renderSectionManager.renderLayer(matrices, DefaultTerrainRenderPasses.SOLID, x, y, z);
            this.renderSectionManager.renderLayer(matrices, DefaultTerrainRenderPasses.CUTOUT, x, y, z);
        } else if (renderLayer == RenderLayer.getTranslucent()) {
            this.renderSectionManager.renderLayer(matrices, DefaultTerrainRenderPasses.TRANSLUCENT, x, y, z);
        }
    }

    public void reload() {
        if (this.world == null) {
            return;
        }

        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            this.initRenderer(commandList);
        }
    }

    private void initRenderer(CommandList commandList) {
        if (this.renderSectionManager != null) {
            this.renderSectionManager.destroy();
            this.renderSectionManager = null;
        }

        this.renderDistance = this.client.options.getClampedViewDistance();

        this.renderSectionManager = new RenderSectionManager(this.world, this.renderDistance, commandList);

        var tracker = ChunkTrackerHolder.get(this.world);
        ChunkTracker.forEachChunk(tracker.getReadyChunks(), this.renderSectionManager::onChunkAdded);
    }

    public void renderBlockEntities(MatrixStack matrices,
                                    BufferBuilderStorage bufferBuilders,
                                    Long2ObjectMap<SortedSet<BlockBreakingInfo>> blockBreakingProgressions,
                                    Camera camera,
                                    float tickDelta) {
        VertexConsumerProvider.Immediate immediate = bufferBuilders.getEntityVertexConsumers();

        Vec3d cameraPos = camera.getPos();
        double x = cameraPos.getX();
        double y = cameraPos.getY();
        double z = cameraPos.getZ();

        BlockEntityRenderDispatcher blockEntityRenderer = MinecraftClient.getInstance().getBlockEntityRenderDispatcher();

        this.renderBlockEntities(matrices, bufferBuilders, blockBreakingProgressions, tickDelta, immediate, x, y, z, blockEntityRenderer);
        this.renderGlobalBlockEntities(matrices, bufferBuilders, blockBreakingProgressions, tickDelta, immediate, x, y, z, blockEntityRenderer);
    }

    private void renderBlockEntities(MatrixStack matrices,
                                     BufferBuilderStorage bufferBuilders,
                                     Long2ObjectMap<SortedSet<BlockBreakingInfo>> blockBreakingProgressions,
                                     float tickDelta,
                                     VertexConsumerProvider.Immediate immediate,
                                     double x,
                                     double y,
                                     double z,
                                     BlockEntityRenderDispatcher blockEntityRenderer) {
        SortedRenderLists renderLists = this.renderSectionManager.getRenderLists();
        Iterator<ChunkRenderList> renderListIterator = renderLists.iterator();

        while (renderListIterator.hasNext()) {
            var renderList = renderListIterator.next();

            var renderRegion = renderList.getRegion();
            var renderSectionIterator = renderList.sectionsWithEntitiesIterator();

            if (renderSectionIterator == null) {
                continue;
            }

            while (renderSectionIterator.hasNext()) {
                var renderSectionId = renderSectionIterator.nextByteAsInt();
                var renderSection = renderRegion.getSection(renderSectionId);

                var blockEntities = renderSection.getCulledBlockEntities();

                if (blockEntities == null) {
                    continue;
                }

                for (BlockEntity blockEntity : blockEntities) {
                    renderBlockEntity(matrices, bufferBuilders, blockBreakingProgressions, tickDelta, immediate, x, y, z, blockEntityRenderer, blockEntity);
                }
            }
        }
    }

    private void renderGlobalBlockEntities(MatrixStack matrices,
                                           BufferBuilderStorage bufferBuilders,
                                           Long2ObjectMap<SortedSet<BlockBreakingInfo>> blockBreakingProgressions,
                                           float tickDelta,
                                           VertexConsumerProvider.Immediate immediate,
                                           double x,
                                           double y,
                                           double z,
                                           BlockEntityRenderDispatcher blockEntityRenderer) {
        for (var renderSection : this.renderSectionManager.getSectionsWithGlobalEntities()) {
            var blockEntities = renderSection.getGlobalBlockEntities();

            if (blockEntities == null) {
                continue;
            }

            for (var blockEntity : blockEntities) {
                renderBlockEntity(matrices, bufferBuilders, blockBreakingProgressions, tickDelta, immediate, x, y, z, blockEntityRenderer, blockEntity);
            }
        }
    }

    private static void renderBlockEntity(MatrixStack matrices,
                                          BufferBuilderStorage bufferBuilders,
                                          Long2ObjectMap<SortedSet<BlockBreakingInfo>> blockBreakingProgressions,
                                          float tickDelta,
                                          VertexConsumerProvider.Immediate immediate,
                                          double x,
                                          double y,
                                          double z,
                                          BlockEntityRenderDispatcher dispatcher,
                                          BlockEntity entity) {
        BlockPos pos = entity.getPos();

        matrices.push();
        matrices.translate((double) pos.getX() - x, (double) pos.getY() - y, (double) pos.getZ() - z);

        VertexConsumerProvider consumer = immediate;
        SortedSet<BlockBreakingInfo> breakingInfo = blockBreakingProgressions.get(pos.asLong());

        if (breakingInfo != null && !breakingInfo.isEmpty()) {
            int stage = breakingInfo.last().getStage();

            if (stage >= 0) {
                var bufferBuilder = bufferBuilders.getEffectVertexConsumers()
                        .getBuffer(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(stage));

                MatrixStack.Entry entry = matrices.peek();
                VertexConsumer transformer = new OverlayVertexConsumer(bufferBuilder,
                        entry.getPositionMatrix(), entry.getNormalMatrix(), 1.0f);

                consumer = (layer) -> layer.hasCrumbling() ? VertexConsumers.union(transformer, immediate.getBuffer(layer)) : immediate.getBuffer(layer);
            }
        }

        dispatcher.render(entity, tickDelta, matrices, consumer);

        matrices.pop();
    }

    // the volume of a section multiplied by the number of sections to be checked at most
    private static final double MAX_ENTITY_CHECK_VOLUME = 16 * 16 * 16 * 15;

    /**
     * Returns whether or not the entity intersects with any visible chunks in the graph.
     * @return True if the entity is visible, otherwise false
     */
    public boolean isEntityVisible(Entity entity) {
        if (!this.useEntityCulling) {
            return true;
        }

        // Ensure entities with outlines or nametags are always visible
        if (this.client.hasOutline(entity) || entity.shouldRenderName()) {
            return true;
        }

        Box box = entity.getVisibilityBoundingBox();

        // bail on very large entities to avoid checking many sections
        double entityVolume = (box.maxX - box.minX) * (box.maxY - box.minY) * (box.maxZ - box.minZ);
        if (entityVolume > MAX_ENTITY_CHECK_VOLUME) {
            // TODO: do a frustum check instead, even large entities aren't visible if they're outside the frustum
            return true;
        }

        return this.isBoxVisible(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    public boolean isBoxVisible(double x1, double y1, double z1, double x2, double y2, double z2) {
        // Boxes outside the valid world height will never map to a rendered chunk
        // Always render these boxes or they'll be culled incorrectly!
        if (y2 < this.world.getBottomY() + 0.5D || y1 > this.world.getTopY() - 0.5D) {
            return true;
        }

        int minX = ChunkSectionPos.getSectionCoord(x1 - 0.5D);
        int minY = ChunkSectionPos.getSectionCoord(y1 - 0.5D);
        int minZ = ChunkSectionPos.getSectionCoord(z1 - 0.5D);

        int maxX = ChunkSectionPos.getSectionCoord(x2 + 0.5D);
        int maxY = ChunkSectionPos.getSectionCoord(y2 + 0.5D);
        int maxZ = ChunkSectionPos.getSectionCoord(z2 + 0.5D);

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
        // C: visible/total D: distance
        // TODO: add dirty and queued counts
        return String.format("C: %d/%d D: %d", this.renderSectionManager.getVisibleChunkCount(), this.renderSectionManager.getTotalSections(), this.renderDistance);
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

    public Collection<String> getDebugStrings() {
        return this.renderSectionManager.getDebugStrings();
    }

    public boolean isSectionReady(int x, int y, int z) {
        return this.renderSectionManager.isSectionBuilt(x, y, z);
    }
}
