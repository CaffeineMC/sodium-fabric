package me.jellysquid.mods.sodium.client.render.chunk;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.graph.ChunkGraphIterationQueue;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderListBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
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
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.*;
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

    /**
     * The minimum distance the culling plane can be from the player's camera. This helps to prevent mathematical
     * errors that occur when the fog distance is less than 8 blocks in width, such as when using a blindness potion.
     */
    private static final float FOG_PLANE_MIN_DISTANCE = (float) Math.pow(8.0f, 2.0);

    /**
     * The distance past the fog's far plane at which to begin culling. Distance calculations use the center of each
     * chunk from the camera's position, and as such, special care is needed to ensure that the culling plane is pushed
     * back far enough. I'm sure there's a mathematical formula that should be used here in place of the constant,
     * but this value works fine in testing.
     */
    private static final float FOG_PLANE_OFFSET = 12.0f;

    private final ChunkBuilder builder;

    private final RenderRegionManager regions;
    private final ClonedChunkSectionCache sectionCache;

    private final Long2ReferenceOpenHashMap<RenderSection> sections = new Long2ReferenceOpenHashMap<>();

    private final EnumMap<ChunkUpdateType, PriorityQueue<RenderSection>> rebuildQueues = new EnumMap<>(ChunkUpdateType.class);

    private final ChunkGraphIterationQueue iterationQueue = new ChunkGraphIterationQueue();

    private final ObjectArrayList<RenderSection> tickableChunks = new ObjectArrayList<>();
    private final ObjectArrayList<BlockEntity> visibleBlockEntities = new ObjectArrayList<>();

    private final RegionChunkRenderer chunkRenderer;

    private final SodiumWorldRenderer worldRenderer;
    private final ClientWorld world;

    private final ChunkTracker chunkTracker;

    private final int renderDistance;

    private ChunkTracker.Area watchedArea;

    private float cameraX, cameraY, cameraZ;
    private int centerChunkX, centerChunkZ;

    private boolean needsUpdate;

    private boolean useFogCulling;
    private boolean useOcclusionCulling;

    private float fogRenderCutoff;

    private Frustum frustum;

    private int currentFrame = 0;
    private boolean alwaysDeferChunkUpdates;

    private ChunkRenderList chunkRenderList;

    public RenderSectionManager(SodiumWorldRenderer worldRenderer, ClientWorld world, int renderDistance, ChunkTracker chunkTracker, CommandList commandList) {
        this.chunkRenderer = new RegionChunkRenderer(RenderDevice.INSTANCE, ChunkMeshFormats.COMPACT);

        this.worldRenderer = worldRenderer;
        this.world = world;

        this.builder = new ChunkBuilder(ChunkMeshFormats.COMPACT);
        this.builder.init(world);

        this.needsUpdate = true;
        this.renderDistance = renderDistance;

        this.regions = new RenderRegionManager(commandList);
        this.sectionCache = new ClonedChunkSectionCache(this.world);

        for (ChunkUpdateType type : ChunkUpdateType.values()) {
            this.rebuildQueues.put(type, new ObjectArrayFIFOQueue<>());
        }

        this.chunkTracker = chunkTracker;
    }

    public void update(Camera camera, Frustum frustum, int frame, boolean spectator) {
        this.updateWatchedArea(camera);
        this.processChunkQueues();

        this.resetLists();

        var list = new ChunkRenderListBuilder();

        this.setup(camera);
        this.iterateChunks(list, camera, frustum, frame, spectator);

        this.chunkRenderList = list.build();
        this.needsUpdate = false;
    }

    private void setup(Camera camera) {
        Vec3d cameraPos = camera.getPos();

        this.cameraX = (float) cameraPos.x;
        this.cameraY = (float) cameraPos.y;
        this.cameraZ = (float) cameraPos.z;

        var options = SodiumClientMod.options();

        this.useFogCulling = options.performance.useFogOcclusion;
        this.alwaysDeferChunkUpdates = options.performance.alwaysDeferChunkUpdates;

        if (this.useFogCulling) {
            float dist = RenderSystem.getShaderFogEnd() + FOG_PLANE_OFFSET;

            if (dist == 0.0f) {
                this.fogRenderCutoff = Float.POSITIVE_INFINITY;
            } else {
                this.fogRenderCutoff = Math.max(FOG_PLANE_MIN_DISTANCE, dist * dist);
            }
        }
    }

    private void iterateChunks(ChunkRenderListBuilder list, Camera camera, Frustum frustum, int frame, boolean spectator) {
        ChunkGraphIterationQueue queue = this.iterationQueue;

        this.initSearch(queue, camera, frustum, frame, spectator);

        while (!queue.isEmpty()) {
            RenderSection section = queue.getSection();

            int incomingDirection = queue.getDirection();
            int incomingCullState = queue.getCullState();

            queue.next();

            this.addToRenderLists(list, section);

            for (int outgoingDirection : GraphDirection.DIRECTIONS) {
                if ((incomingCullState & (1 << outgoingDirection)) != 0) {
                    continue;
                }

                if (this.useOcclusionCulling && !section.isVisibleThrough(incomingDirection, outgoingDirection)) {
                    continue;
                }

                RenderSection adjSection = section.getAdjacent(outgoingDirection);

                if (adjSection != null && this.isWithinRenderDistance(adjSection)) {
                    this.bfsEnqueue(queue, adjSection, GraphDirection.opposite(outgoingDirection), incomingCullState);
                }
            }
        }
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

    private void schedulePendingUpdates(RenderSection section) {
        var pendingUpdate = section.getPendingUpdate();

        if (pendingUpdate == null) {
            return;
        }

        var queue = this.rebuildQueues.get(pendingUpdate);

        if (queue.size() < 32) {
            queue.enqueue(section);
        }
    }

    private void addEntitiesToRenderLists(RenderSection render) {
        Collection<BlockEntity> blockEntities = render.getData()
                .getBlockEntities();

        if (!blockEntities.isEmpty()) {
            this.visibleBlockEntities.addAll(blockEntities);
        }
    }

    private void resetLists() {
        for (PriorityQueue<RenderSection> queue : this.rebuildQueues.values()) {
            queue.clear();
        }

        this.visibleBlockEntities.clear();
        this.tickableChunks.clear();

        this.chunkRenderList = null;
    }

    public Collection<BlockEntity> getVisibleBlockEntities() {
        return this.visibleBlockEntities;
    }

    private void loadSection(int x, int y, int z) {
        RenderSection render = new RenderSection(this.worldRenderer, x, y, z);

        if (this.sections.putIfAbsent(ChunkSectionPos.asLong(x, y, z), render) != null) {
            throw new RuntimeException("Section is already loaded at {x=%s,y=%s,z=%s".formatted(x, y, z));
        }

        Chunk chunk = this.world.getChunk(x, z);
        ChunkSection section = chunk.getSectionArray()[this.world.sectionCoordToIndex(y)];

        if (section.isEmpty()) {
            render.setData(ChunkRenderData.EMPTY);
        } else {
            render.markForUpdate(ChunkUpdateType.INITIAL_BUILD);
        }

        this.connectNeighborNodes(render);
    }

    private void unloadSection(int x, int y, int z) {
        RenderSection chunk = this.sections.remove(ChunkSectionPos.asLong(x, y, z));

        if (chunk == null) {
            throw new RuntimeException("Section is not loaded at {x=%s,y=%s,z=%s".formatted(x, y, z));
        }

        chunk.delete();

        RenderRegion region = this.regions.getRegion(RenderRegion.getRegionKeyForChunk(x, y, z));

        if (region != null) {
            region.deleteSection(chunk);
        }

        this.disconnectNeighborNodes(chunk);
    }

    public void renderLayer(ChunkRenderMatrices matrices, TerrainRenderPass pass, double x, double y, double z) {
        Validate.notNull(this.chunkRenderList, "Render list is null");

        RenderDevice device = RenderDevice.INSTANCE;
        CommandList commandList = device.createCommandList();

        this.chunkRenderer.render(matrices, commandList, this.regions, this.chunkRenderList, pass, new ChunkCameraContext(x, y, z));

        commandList.flush();
    }

    public void tickVisibleRenders() {
        for (RenderSection render : this.tickableChunks) {
            render.tickAnimatedSprites();
        }
    }

    public boolean isSectionVisible(int x, int y, int z) {
        RenderSection render = this.getRenderSection(x, y, z);

        if (render == null) {
            return false;
        }

        return render.getLastVisibleFrame() == this.currentFrame;
    }

    public void updateChunks() {
        updateChunks(false);
    }

    public void updateAllChunksNow() {
        updateChunks(true);

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

        PriorityQueue<RenderSection> queue = this.rebuildQueues.get(filterType);

        while (budget > 0 && !queue.isEmpty()) {
            RenderSection section = queue.dequeue();

            if (section.isDisposed()) {
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
        int frame = this.currentFrame;

        if (context == null) {
            return new ChunkRenderEmptyBuildTask(render, frame);
        }

        return new ChunkRenderRebuildTask(render, context, frame);
    }

    public void markGraphDirty() {
        this.needsUpdate = true;
    }

    public boolean isGraphDirty() {
        return true;
    }

    public ChunkBuilder getBuilder() {
        return this.builder;
    }

    public void destroy() {
        this.resetLists();

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
        return this.sections.size();
    }

    public int getVisibleChunkCount() {
        return this.chunkRenderList.getCount();
    }

    public void scheduleRebuild(int x, int y, int z, boolean important) {
        this.sectionCache.invalidate(x, y, z);

        RenderSection section = this.sections.get(ChunkSectionPos.asLong(x, y, z));

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

    public void onChunkRenderUpdates(int x, int y, int z, ChunkRenderData data) {
        RenderSection node = this.getRenderSection(x, y, z);

        if (node != null) {
            node.setOcclusionData(data.getOcclusionData());
        }
    }

    private boolean isWithinRenderDistance(RenderSection adj) {
        int x = Math.abs(adj.getChunkX() - this.centerChunkX);
        int z = Math.abs(adj.getChunkZ() - this.centerChunkZ);

        return x <= this.renderDistance && z <= this.renderDistance;
    }

    private void initSearch(ChunkGraphIterationQueue queue, Camera camera, Frustum frustum, int frame, boolean spectator) {
        this.currentFrame = frame;
        this.frustum = frustum;
        this.useOcclusionCulling = MinecraftClient.getInstance().chunkCullingEnabled;

        this.iterationQueue.clear();

        BlockPos origin = camera.getBlockPos();

        int chunkX = origin.getX() >> 4;
        int chunkY = origin.getY() >> 4;
        int chunkZ = origin.getZ() >> 4;

        this.centerChunkX = chunkX;
        this.centerChunkZ = chunkZ;

        RenderSection rootRender = this.getRenderSection(chunkX, chunkY, chunkZ);

        if (rootRender != null) {
            rootRender.setLastVisibleFrame(frame);

            if (spectator && this.world.getBlockState(origin).isOpaqueFullCube(this.world, origin)) {
                this.useOcclusionCulling = false;
            }

            queue.add(rootRender, GraphDirection.NONE, 0);
        } else {
            chunkY = MathHelper.clamp(origin.getY() >> 4, this.world.getBottomSectionCoord(), this.world.getTopSectionCoord() - 1);

            List<RenderSection> sorted = new ArrayList<>();

            for (int x2 = -this.renderDistance; x2 <= this.renderDistance; ++x2) {
                for (int z2 = -this.renderDistance; z2 <= this.renderDistance; ++z2) {
                    RenderSection render = this.getRenderSection(chunkX + x2, chunkY, chunkZ + z2);

                    if (render == null) {
                        continue;
                    }

                    if (render.isCulledByFrustum(frustum)) {
                        continue;
                    }

                    render.setLastVisibleFrame(frame);

                    sorted.add(render);
                }
            }

            sorted.sort(Comparator.comparingDouble(node -> node.getSquaredDistance(origin)));

            for (RenderSection render : sorted) {
                queue.add(render, GraphDirection.NONE, 0);
            }
        }
    }


    private void bfsEnqueue(ChunkGraphIterationQueue queue, RenderSection render, int incomingDirection, int cullState) {
        if (render.getLastVisibleFrame() == this.currentFrame) {
            return;
        }

        render.setLastVisibleFrame(this.currentFrame);

        if (render.isCulledByFrustum(this.frustum)) {
            return;
        }

        queue.add(render, incomingDirection, cullState | (1 << incomingDirection));
    }

    private void addToRenderLists(ChunkRenderListBuilder list, RenderSection render) {
        this.schedulePendingUpdates(render);

        if (this.useFogCulling && render.getSquaredDistanceXZ(this.cameraX, this.cameraZ) >= this.fogRenderCutoff) {
            return;
        }

        int flags = render.getFlags();

        if ((flags & RenderSectionFlags.HAS_BLOCK_GEOMETRY) != 0) {
            list.add(render);
        }

        if ((flags & RenderSectionFlags.HAS_BLOCK_ENTITIES) != 0) {
            this.addEntitiesToRenderLists(render);
        }

        if ((flags & RenderSectionFlags.HAS_ANIMATED_SPRITES) != 0) {
            this.tickableChunks.add(render);
        }
    }

    @Deprecated
    private void connectNeighborNodes(RenderSection render) {
        for (int dir : GraphDirection.DIRECTIONS) {
            RenderSection adj = this.getRenderSection(render.getChunkX() + GraphDirection.x(dir),
                    render.getChunkY() + GraphDirection.y(dir),
                    render.getChunkZ() + GraphDirection.z(dir));

            if (adj != null) {
                adj.setAdjacentNode(GraphDirection.opposite(dir), render);
                render.setAdjacentNode(dir, adj);
            }
        }
    }

    @Deprecated
    private void disconnectNeighborNodes(RenderSection render) {
        for (int dir : GraphDirection.DIRECTIONS) {
            RenderSection adj = render.getAdjacent(dir);

            if (adj != null) {
                adj.setAdjacentNode(GraphDirection.opposite(dir), null);
                render.setAdjacentNode(dir, null);
            }
        }
    }

    private RenderSection getRenderSection(int x, int y, int z) {
        return this.sections.get(ChunkSectionPos.asLong(x, y, z));
    }

    public Collection<String> getDebugStrings() {
        List<String> list = new ArrayList<>();

        int count = 0;

        long deviceUsed = 0;
        long deviceAllocated = 0;

        for (var region : this.regions.getLoadedRegions()) {
            deviceUsed += region.getDeviceUsedMemory();
            deviceAllocated += region.getDeviceAllocatedMemory();

            count++;
        }

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

    private RenderSection getSection(int x, int y, int z) {
        return this.sections.get(ChunkSectionPos.asLong(x, y, z));
    }

    public boolean isSectionReady(int x, int y, int z) {
        var section = this.getSection(x, y, z);

        if (section != null) {
            return section.getData() != ChunkRenderData.ABSENT;
        }

        return false;
    }
}
