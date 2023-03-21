package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongIterator;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import me.jellysquid.mods.sodium.client.render.chunk.graph.GraphSearch;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionManager;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderBuildTask;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderEmptyBuildTask;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderRebuildTask;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkMeshFormats;
import me.jellysquid.mods.sodium.client.util.MathUtil;
import me.jellysquid.mods.sodium.client.util.frustum.Frustum;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSectionCache;
import me.jellysquid.mods.sodium.common.util.collections.WorkStealingFutureDrain;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import org.apache.commons.lang3.Validate;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class RenderSectionManager {
    /**
     * The maximum distance a chunk can be from the player's camera in order to be eligible for blocking updates.
     */
    private static final double NEARBY_CHUNK_DISTANCE = Math.pow(32, 2.0);

    private final ChunkBuilder builder;

    private final RenderRegionManager regions;
    private final ClonedChunkSectionCache sectionCache;
    private final RegionChunkRenderer chunkRenderer;

    private final ClientWorld world;

    private final ChunkTracker chunkTracker;

    private final int renderDistance;

    private ChunkTracker.Area watchedArea;

    private float cameraX, cameraY, cameraZ;

    private boolean needsUpdate;

    private boolean alwaysDeferChunkUpdates;

    private ChunkRenderList renderList = ChunkRenderList.empty();
    private final SodiumWorldRenderer worldRenderer;

    private final GraphSearch.GraphSearchPool graphSearchPool = new GraphSearch.GraphSearchPool();

    public RenderSectionManager(SodiumWorldRenderer worldRenderer, ClientWorld world, int renderDistance, ChunkTracker chunkTracker, CommandList commandList) {
        this.worldRenderer = worldRenderer;
        this.chunkRenderer = new RegionChunkRenderer(RenderDevice.INSTANCE, ChunkMeshFormats.COMPACT);

        this.world = world;

        this.builder = new ChunkBuilder(ChunkMeshFormats.COMPACT);
        this.builder.init(world);

        this.needsUpdate = true;
        this.renderDistance = renderDistance;

        this.regions = new RenderRegionManager(commandList);
        this.sectionCache = new ClonedChunkSectionCache(this.world);

        this.chunkTracker = chunkTracker;
    }

    public void update(Camera camera, Frustum frustum, int frame, boolean spectator) {
        this.setup(camera);

        this.updateWatchedArea(camera);
        this.processChunkQueues();

        if (this.renderList != null) {
            this.graphSearchPool.releaseRenderList(this.renderList);
            this.renderList = null;
        }

        var useOcclusionCulling = MinecraftClient.getInstance().chunkCullingEnabled;

        if (spectator && this.world.getBlockState(camera.getBlockPos())
                .isOpaqueFullCube(this.world, camera.getBlockPos())) {
            useOcclusionCulling = false;
        }

        this.renderList = new GraphSearch(this.graphSearchPool, this.regions, camera, frustum, this.renderDistance, useOcclusionCulling)
                .getVisible();
        this.needsUpdate = false;
    }

    private void setup(Camera camera) {
        Vec3d cameraPos = camera.getPos();

        this.cameraX = (float) cameraPos.x;
        this.cameraY = (float) cameraPos.y;
        this.cameraZ = (float) cameraPos.z;

        this.alwaysDeferChunkUpdates = SodiumClientMod.options().performance.alwaysDeferChunkUpdates;
    }

    private void updateWatchedArea(Camera camera) {
        int x = camera.getBlockPos().getX() >> 4;
        int z = camera.getBlockPos().getZ() >> 4;

        var area = new ChunkTracker.Area(x, z, this.renderDistance);

        if (Objects.equals(this.watchedArea, area)) {
            return;
        }

        if (this.watchedArea != null) {
            this.chunkTracker.updateWatchedArea(this.watchedArea, area);
        } else {
            this.chunkTracker.addWatchedArea(area);
        }

        this.watchedArea = area;
    }

    private LongArrayFIFOQueue queue = new LongArrayFIFOQueue();

    private void loadSection(int x, int y, int z) {
        RenderSection section = this.regions.loadSection(this.worldRenderer, x, y, z);

        Chunk worldChunk = this.world.getChunk(x, z);
        ChunkSection worldSection = worldChunk.getSectionArray()[this.world.sectionCoordToIndex(y)];

        if (worldSection.isEmpty()) {
            section.setData(BuiltSectionInfo.EMPTY);
        } else {
            section.markForUpdate(ChunkUpdateType.INITIAL_BUILD);

            // TODO: TERRIBLE HACK TO MAKE THINGS LOAD
            this.queue.enqueue(ChunkSectionPos.asLong(x, y, z));
        }
    }

    private void unloadSection(int x, int y, int z) {
        this.regions.unloadSection(x, y, z);
    }

    public void renderLayer(ChunkRenderMatrices matrices, TerrainRenderPass pass, double x, double y, double z) {
        Validate.notNull(this.renderList, "Render list is null");

        RenderDevice device = RenderDevice.INSTANCE;
        CommandList commandList = device.createCommandList();

        this.chunkRenderer.render(matrices, commandList, this.renderList, pass, new ChunkCameraContext(x, y, z));

        commandList.flush();
    }

    public void tickVisibleRenders() {
        this.renderList.forEachSectionWithSprites(RenderSection::tickAnimatedSprites);
    }

    public boolean isSectionVisible(int x, int y, int z) {
        // TODO: Fix me
        return true;
    }

    public void updateChunks() {
        this.updateChunks(false);
    }

    public void updateAllChunksNow() {
        this.updateChunks(true);

        // Also wait for any rebuilds which had already been scheduled before this method was called
        this.needsUpdate |= this.performAllUploads();
    }

    private void updateChunks(boolean allImmediately) {
        var blockingFutures = new LinkedList<CompletableFuture<ChunkBuildResult>>();

        this.submitRebuildTasks(ChunkUpdateType.IMPORTANT_REBUILD, blockingFutures);
        this.submitRebuildTasks(ChunkUpdateType.INITIAL_BUILD, allImmediately ? blockingFutures : null);
        this.submitRebuildTasks(ChunkUpdateType.REBUILD, allImmediately ? blockingFutures : null);

        // Try to complete some other work on the main thread while we wait for rebuilds to complete
        this.needsUpdate |= this.performPendingUploads();

        if (!blockingFutures.isEmpty()) {
            this.needsUpdate = true;
            this.regions.upload(RenderDevice.INSTANCE.createCommandList(), new WorkStealingFutureDrain<>(blockingFutures, this.builder::stealTask));
        }

        this.regions.cleanup();
    }

    private void submitRebuildTasks(ChunkUpdateType filterType, LinkedList<CompletableFuture<ChunkBuildResult>> immediateFutures) {
        int budget = immediateFutures != null ? Integer.MAX_VALUE : this.builder.getSchedulingBudget();

        if (filterType != ChunkUpdateType.INITIAL_BUILD) {
            return;
        }
        LongArrayFIFOQueue queue = this.queue;

        while (budget > 0 && !queue.isEmpty()) {
            var sectionPos = queue.dequeueLong();
            var section = this.regions.getSection(ChunkSectionPos.unpackX(sectionPos),
                    ChunkSectionPos.unpackY(sectionPos),
                    ChunkSectionPos.unpackZ(sectionPos));

            if (section == null) {
                continue;
            }

            // Sections can move between update queues, but they won't be removed from the queue they were
            // previously in to save CPU cycles. We just filter any changed entries here instead.
            if (section.getPendingUpdate() != filterType) {
                continue;
            }

            ChunkRenderBuildTask task = this.createRebuildTask(section);
            CompletableFuture<?> future;

            if (immediateFutures != null) {
                CompletableFuture<ChunkBuildResult> immediateFuture = this.builder.schedule(task);
                immediateFutures.add(immediateFuture);

                future = immediateFuture;
            } else {
                future = this.builder.scheduleDeferred(task);
            }

            section.onBuildSubmitted(future);

            budget--;
        }
    }

    private boolean performPendingUploads() {
        Iterator<ChunkBuildResult> it = this.builder.createDeferredBuildResultDrain();

        if (!it.hasNext()) {
            return false;
        }

        this.regions.upload(RenderDevice.INSTANCE.createCommandList(), it);

        return true;
    }

    /**
     * Processes all build task uploads, blocking for tasks to complete if necessary.
     */
    private boolean performAllUploads() {
        boolean anythingUploaded = false;

        while (true) {
            // First check if all tasks are done building (and therefore the upload queue is final)
            boolean allTasksBuilt = this.builder.isIdle();

            // Then process the entire upload queue
            anythingUploaded |= this.performPendingUploads();

            // If the upload queue was the final one
            if (allTasksBuilt) {
                // then we are done
                return anythingUploaded;
            } else {
                // otherwise we need to wait for the worker threads to make progress
                try {
                    // This code path is not the default one, it doesn't need super high performance, and having the
                    // workers notify the main thread just for it is probably not worth it.
                    //noinspection BusyWait
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return true;
                }
            }
        }
    }

    public ChunkRenderBuildTask createRebuildTask(RenderSection render) {
        ChunkRenderContext context = WorldSlice.prepare(this.world, render.getChunkPos(), this.sectionCache);
        int frame = 0; // TODO: fix me

        if (context == null) {
            return new ChunkRenderEmptyBuildTask(render, frame);
        }

        return new ChunkRenderRebuildTask(render, context, frame);
    }

    public void markGraphDirty() {
        this.needsUpdate = true;
    }

    public boolean isGraphDirty() {
        return this.needsUpdate;
    }

    public ChunkBuilder getBuilder() {
        return this.builder;
    }

    public void destroy() {
        this.renderList = null;

        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            this.regions.delete(commandList);
            this.chunkRenderer.delete(commandList);
        }

        if (this.watchedArea != null) {
            this.chunkTracker.removeWatchedArea(this.watchedArea);
        }

        this.builder.stopWorkers();
    }

    public int getTotalSections() {
        return this.regions.sectionCount();
    }

    public int getVisibleChunkCount() {
        return this.renderList.size();
    }

    public void scheduleRebuild(int x, int y, int z, boolean important) {
        this.sectionCache.invalidate(x, y, z);

        RenderSection section = this.regions.getSection(x, y, z);

        if (section != null && section.isBuilt()) {
            if (!this.alwaysDeferChunkUpdates && (important || this.isChunkPrioritized(section))) {
                section.markForUpdate(ChunkUpdateType.IMPORTANT_REBUILD);
            } else {
                section.markForUpdate(ChunkUpdateType.REBUILD);
            }
        }

        this.needsUpdate = true;
    }

    public boolean isChunkPrioritized(RenderSection render) {
        return render.getSquaredDistance(this.cameraX, this.cameraY, this.cameraZ) <= NEARBY_CHUNK_DISTANCE;
    }

    public void onChunkRenderUpdates(int x, int y, int z, RenderSection section, BuiltSectionInfo data) {
        if (section.region != null) { // TODO: terrible hack, we shouldn't even get here if this was unloaded already
            section.region.updateNode(section, data);
        }
    }

    public Collection<String> getDebugStrings() {
        int count = 0;

        long deviceUsed = 0;
        long deviceAllocated = 0;

        for (var region : this.regions.getLoadedRegions()) {
            var resources = region.getResources();

            if (resources == null) {
                continue;
            }

            deviceUsed += resources.getDeviceUsedMemory();
            deviceAllocated += resources.getDeviceAllocatedMemory();

            count++;
        }

        List<String> list = new ArrayList<>();
        list.add(String.format("Device buffer objects: %d", count));
        list.add(String.format("Device memory: %d/%d MiB", MathUtil.toMib(deviceUsed), MathUtil.toMib(deviceAllocated)));

        list.add(String.format("Staging buffer: %s", this.regions.getStagingBuffer().toString()));

        return list;
    }

    public void processChunkQueues() {
        var queue = this.chunkTracker.getEvents(this.watchedArea);

        for (LongIterator iterator = queue.removed().iterator(); iterator.hasNext(); ) {
            long pos = iterator.nextLong();

            int x = ChunkPos.getPackedX(pos);
            int z = ChunkPos.getPackedZ(pos);

            for (int y = this.world.getBottomSectionCoord(); y < this.world.getTopSectionCoord(); y++) {
                this.unloadSection(x, y, z);
            }
        }

        for (LongIterator iterator = queue.added().iterator(); iterator.hasNext(); ) {
            long pos = iterator.nextLong();

            int x = ChunkPos.getPackedX(pos);
            int z = ChunkPos.getPackedZ(pos);

            for (int y = this.world.getBottomSectionCoord(); y < this.world.getTopSectionCoord(); y++) {
                this.loadSection(x, y, z);
            }
        }
    }

    public boolean isSectionReady(int x, int y, int z) {
        var section = this.regions.getSection(x, y, z);

        if (section != null) {
            return section.getData() != BuiltSectionInfo.ABSENT;
        }

        return false;
    }

    public ChunkRenderList getRenderList() {
        return this.renderList;
    }
}
