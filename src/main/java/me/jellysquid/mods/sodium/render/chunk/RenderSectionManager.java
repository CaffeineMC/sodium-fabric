package me.jellysquid.mods.sodium.render.chunk;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongPriorityQueue;
import me.jellysquid.mods.sodium.SodiumClient;
import me.jellysquid.mods.sodium.interop.vanilla.world.ChunkStatusListener;
import me.jellysquid.mods.sodium.interop.vanilla.world.ClientChunkManagerExtended;
import me.jellysquid.mods.sodium.model.quad.properties.ModelQuadFacingBits;
import me.jellysquid.mods.sodium.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.render.chunk.compile.ChunkBuilder;
import me.jellysquid.mods.sodium.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.render.chunk.format.ModelVertexType;
import me.jellysquid.mods.sodium.render.chunk.graph.ChunkAdjacencyMap;
import me.jellysquid.mods.sodium.render.chunk.graph.ChunkIterationQueue;
import me.jellysquid.mods.sodium.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.render.chunk.region.RenderRegionManager;
import me.jellysquid.mods.sodium.render.chunk.region.RenderRegionStorage;
import me.jellysquid.mods.sodium.render.chunk.tasks.ChunkRenderBuildTask;
import me.jellysquid.mods.sodium.render.chunk.tasks.ChunkRenderEmptyBuildTask;
import me.jellysquid.mods.sodium.render.chunk.tasks.ChunkRenderRebuildTask;
import me.jellysquid.mods.sodium.render.chunk.tree.ChunkGraph;
import me.jellysquid.mods.sodium.render.chunk.tree.ChunkGraphState;
import me.jellysquid.mods.sodium.util.DirectionUtil;
import me.jellysquid.mods.sodium.util.MathUtil;
import me.jellysquid.mods.sodium.util.collections.WorkStealingFutureDrain;
import me.jellysquid.mods.sodium.world.WorldSlice;
import me.jellysquid.mods.sodium.world.cloned.ChunkRenderContext;
import me.jellysquid.mods.sodium.world.cloned.ClonedChunkSectionCache;
import me.jellysquid.mods.thingl.device.RenderDevice;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.*;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import org.joml.FrustumIntersection;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class RenderSectionManager implements ChunkStatusListener {
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

    private final ChunkGraph<RenderSection> graph;

    private final ChunkUpdateQueue updateQueue = new ChunkUpdateQueue();
    private final ChunkAdjacencyMap adjacencyMap = new ChunkAdjacencyMap();

    private final ClientWorld world;

    private final int renderDistance;

    private float cameraX, cameraY, cameraZ;
    private int centerChunkX, centerChunkY, centerChunkZ;

    private boolean needsUpdate;

    private double fogRenderCutoff;

    private int currentFrame = 0;
    private final double detailFarPlane;

    private boolean useFogCulling;
    private boolean useOcclusionCulling;
    private boolean useBlockFaceCulling;
    private boolean alwaysDeferChunkUpdates;

    @Deprecated(forRemoval = true)
    private final ChunkGraphState cachedGraphState = new ChunkGraphState();

    public RenderSectionManager(ClientWorld world, int renderDistance, RenderDevice device) {
        this.world = world;

        this.builder = new ChunkBuilder(ModelVertexType.INSTANCE);
        this.builder.init(world);

        this.needsUpdate = true;
        this.renderDistance = renderDistance;

        this.regions = new RenderRegionManager(device);
        this.graph = new ChunkGraph<>(4, (x, y, z, id) -> {
            var region = this.regions.createRegionForChunk(x, y, z);

            var section = new RenderSection(this, id, x, y, z, region);
            region.addChunk(section);

            return section;
        });

        this.sectionCache = new ClonedChunkSectionCache(this.world);
        this.detailFarPlane = getDetailFarPlane(getDetailDistance(renderDistance));
    }

    private static double getDetailFarPlane(float detailDistance) {
        return Math.pow(detailDistance + 16.0D, 2.0D);
    }

    public void loadChunks() {
        LongIterator it = ((ClientChunkManagerExtended) this.world.getChunkManager())
                .getLoadedChunks()
                .iterator();

        while (it.hasNext()) {
            long pos = it.nextLong();

            this.onChunkAdded(ChunkPos.getPackedX(pos), ChunkPos.getPackedZ(pos));
        }
    }

    public ChunkGraphState update(ChunkRenderList list, Camera camera, FrustumIntersection frustum, int frame, boolean spectator) {
        this.needsUpdate = false;

        this.setup(camera);

        ChunkIterationQueue queue = new ChunkIterationQueue();

        ChunkGraphState state = this.cachedGraphState;
        state.rebuild(this.graph, frustum);

        list.clear();

        this.initSearch(queue, list, camera, frustum, frame, spectator);

        while (!queue.isEmpty()) {
            long next = queue.dequeueNext();

            int renderId = ChunkIterationQueue.getRender(next);
            Direction flow = ChunkIterationQueue.getDirection(next);

            for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
                if (state.canCull(renderId, dir)) {
                    continue;
                }

                // Check that we can traverse along this edge
                if (this.useOcclusionCulling && flow != null && !this.graph.isVisibleThrough(renderId, flow, dir)) {
                    continue;
                }

                var adj = this.graph.getAdjacent(renderId, dir);

                // If the neighbor doesn't exist, skip it
                // If the neighbor has already been made visible or isn't within the frustum, skip it
                if (adj == 0 || !state.canTraverse(adj)) {
                    continue;
                }

                Direction inverse = DirectionUtil.getOpposite(dir);

                state.setCullingState(adj, state.getCullingState(renderId), inverse);
                state.markVisible(adj);

                this.addVisible(queue, list, adj, inverse);
            }
        }

        return state;
    }

    private void setup(Camera camera) {
        Vec3d cameraPos = camera.getPos();

        this.cameraX = (float) cameraPos.x;
        this.cameraY = (float) cameraPos.y;
        this.cameraZ = (float) cameraPos.z;

        var options = SodiumClient.options();

        this.useBlockFaceCulling = options.performance.useBlockFaceCulling;
        this.useFogCulling = options.performance.useFogOcclusion;
        this.alwaysDeferChunkUpdates = options.performance.alwaysDeferChunkUpdates;

        if (this.useFogCulling) {
            float dist = RenderSystem.getShaderFogEnd() + FOG_PLANE_OFFSET;

            if (dist == 0.0f) {
                this.fogRenderCutoff = Double.POSITIVE_INFINITY;
            } else {
                this.fogRenderCutoff = Math.max(FOG_PLANE_MIN_DISTANCE, dist * dist);
            }
        }
    }

    private void addChunkToVisible(ChunkRenderList list, RenderSection render) {
        if (this.useFogCulling && render.getSquaredDistanceXZ(this.cameraX, this.cameraZ) >= this.fogRenderCutoff) {
            return;
        }

        int visibility = ModelQuadFacingBits.UNASSIGNED_BITS;

        if (this.useBlockFaceCulling) {
            visibility |= render.getData()
                    .getBounds()
                    .calculateVisibility(this.cameraX, this.cameraY, this.cameraZ);
        } else {
            visibility |= ModelQuadFacingBits.ALL_BITS;
        }

        list.add(render, visibility);
    }

    @Override
    public void onChunkAdded(int x, int z) {
        this.adjacencyMap.onChunkLoaded(x, z);

        for (int y = this.world.getBottomSectionCoord(); y < this.world.getTopSectionCoord(); y++) {
            this.loadSection(x, y, z);
        }

        this.needsUpdate = true;
    }

    private void trySchedulingChunksForBuild(int originX, int originY, int originZ) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    int adjX = originX + x;
                    int adjY = originY + y;
                    int adjZ = originZ + z;

                    RenderSection section = this.graph.getNode(adjX, adjY, adjZ);

                    if (section == null) {
                        continue;
                    }

                    if (!section.isBuilt() && !this.updateQueue.isQueued(adjX, adjY, adjZ) && this.adjacencyMap.hasNeighbors(adjX, adjZ)) {
                        this.updateQueue.add(adjX, adjY, adjZ, ChunkUpdateType.INITIAL_BUILD);
                    }
                }
            }
        }
    }

    @Override
    public void onChunkRemoved(int x, int z) {
        this.adjacencyMap.onChunkUnloaded(x, z);

        for (int y = this.world.getBottomSectionCoord(); y < this.world.getTopSectionCoord(); y++) {
            this.needsUpdate |= this.unloadSection(x, y, z);
        }
    }

    private void loadSection(int x, int y, int z) {
        var renderId = this.graph.add(x, y, z);
        var render = this.graph.getNode(renderId);

        Chunk chunk = this.world.getChunk(x, z);
        ChunkSection section = chunk.getSectionArray()[this.world.sectionCoordToIndex(y)];

        if (ChunkSection.isEmpty(section)) {
            render.setData(ChunkRenderData.EMPTY);
        }

        this.trySchedulingChunksForBuild(x, y, z);
    }

    private boolean unloadSection(int x, int y, int z) {
        RenderSection chunk = this.graph.getNode(x, y, z);

        if (chunk == null) {
            throw new IllegalStateException("Chunk is not loaded: " + ChunkSectionPos.asLong(x, y, z));
        }

        this.graph.remove(x, y, z, chunk.id);

        chunk.delete();

        RenderRegion region = chunk.getRegion();
        region.removeChunk(chunk);

        this.updateQueue.remove(x, y, z);

        return true;
    }

    public void updateChunks() {
        var sortedQueues = this.updateQueue.sort(this.centerChunkX, this.centerChunkY, this.centerChunkZ);

        var blockingFutures = this.submitRebuildTasks(sortedQueues, ChunkUpdateType.IMPORTANT_REBUILD);

        this.submitRebuildTasks(sortedQueues, ChunkUpdateType.LOD_CHANGE);
        this.submitRebuildTasks(sortedQueues, ChunkUpdateType.INITIAL_BUILD);
        this.submitRebuildTasks(sortedQueues, ChunkUpdateType.REBUILD);

        // Try to complete some other work on the main thread while we wait for rebuilds to complete
        this.needsUpdate |= this.performPendingUploads();

        if (!blockingFutures.isEmpty()) {
            this.needsUpdate = true;
            this.regions.upload(new WorkStealingFutureDrain<>(blockingFutures, this.builder::stealTask));
        }

        this.regions.cleanup();
    }

    private LinkedList<CompletableFuture<ChunkBuildResult>> submitRebuildTasks(LongPriorityQueue[][] queues, ChunkUpdateType filterType) {
        int budget = filterType.isImportant() ? Integer.MAX_VALUE : this.builder.getSchedulingBudget();

        LinkedList<CompletableFuture<ChunkBuildResult>> immediateFutures = new LinkedList<>();

        for (LongPriorityQueue queue : queues[filterType.ordinal()]) {
            while (!queue.isEmpty() && budget-- > 0) {
                RenderSection section = this.graph.getNode(queue.dequeueLong());

                if (section.isDisposed()) {
                    continue;
                }

                ChunkRenderBuildTask task = this.createRebuildTask(section);

                if (filterType.isImportant()) {
                    CompletableFuture<ChunkBuildResult> immediateFuture = this.builder.schedule(task);
                    immediateFutures.add(immediateFuture);
                } else {
                    this.builder.scheduleDeferred(task);
                }
            }
        }

        return immediateFutures;
    }

    private boolean performPendingUploads() {
        Iterator<ChunkBuildResult> it = this.builder.createDeferredBuildResultDrain();

        if (!it.hasNext()) {
            return false;
        }

        this.regions.upload(it);

        return true;
    }

    public ChunkRenderBuildTask createRebuildTask(RenderSection render) {
        ChunkRenderContext context = WorldSlice.prepare(this.world, render.getChunkPos(), this.sectionCache);
        int frame = this.currentFrame;
        int lod = this.getTargetDetailLevel(render);

        if (context == null) {
            return new ChunkRenderEmptyBuildTask(render, frame, lod);
        }

        return new ChunkRenderRebuildTask(render, context, frame, lod);
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
        this.regions.delete();
        this.updateQueue.clear();
        this.builder.stopWorkers();
    }

    public int getTotalSections() {
        int sum = 0;

        for (RenderRegion region : this.regions.getLoadedRegions()) {
            sum += region.getChunkCount();
        }

        return sum;
    }

    public void scheduleRebuild(int x, int y, int z, boolean important) {
        this.sectionCache.invalidate(x, y, z);

        RenderSection render = this.graph.getNode(x, y, z);

        if (render != null && render.isBuilt()) {
            if (!this.alwaysDeferChunkUpdates && (important || this.isChunkPrioritized(render))) {
                this.markChunkForUpdate(ChunkUpdateType.IMPORTANT_REBUILD, render);
            } else {
                this.markChunkForUpdate(ChunkUpdateType.REBUILD, render);
            }
        }

        this.needsUpdate = true;
    }

    public boolean isChunkPrioritized(RenderSection render) {
        return render.getSquaredDistance(this.cameraX, this.cameraY, this.cameraZ) <= NEARBY_CHUNK_DISTANCE;
    }

    private boolean isWithinRenderDistance(int chunkX, int chunkZ) {
        int x = Math.abs(chunkX - this.centerChunkX);
        int z = Math.abs(chunkZ - this.centerChunkZ);

        return x <= this.renderDistance && z <= this.renderDistance;
    }

    private void initSearch(ChunkIterationQueue queue, ChunkRenderList list, Camera camera, FrustumIntersection frustum, int frame, boolean spectator) {
        this.currentFrame = frame;
        this.useOcclusionCulling = MinecraftClient.getInstance().chunkCullingEnabled;

        BlockPos origin = camera.getBlockPos();

        int chunkX = origin.getX() >> 4;
        int chunkY = origin.getY() >> 4;
        int chunkZ = origin.getZ() >> 4;

        this.centerChunkX = chunkX;
        this.centerChunkY = chunkY;
        this.centerChunkZ = chunkZ;

        int rootRender = this.graph.getNodeId(chunkX, chunkY, chunkZ);

        if (rootRender != 0) {
            if (spectator && this.world.getBlockState(origin).isOpaqueFullCube(this.world, origin)) {
                this.useOcclusionCulling = false;
            }

            this.addVisible(queue, list, rootRender, null);
        } else {
            chunkY = MathHelper.clamp(origin.getY() >> 4, this.world.getBottomSectionCoord(), this.world.getTopSectionCoord() - 1);

            List<RenderSection> sorted = new ArrayList<>();

            for (int x2 = -this.renderDistance; x2 <= this.renderDistance; ++x2) {
                for (int z2 = -this.renderDistance; z2 <= this.renderDistance; ++z2) {
                    RenderSection render = this.getRenderSection(chunkX + x2, chunkY, chunkZ + z2);

                    if (render == null || render.isCulledByFrustum(frustum)) {
                        continue;
                    }

                    sorted.add(render);
                }
            }

            sorted.sort(Comparator.comparingDouble(node -> node.getSquaredDistance(origin)));

            for (RenderSection render : sorted) {
                this.addVisible(queue, list, render.id, null);
            }
        }
    }

    private void addVisible(ChunkIterationQueue queue, ChunkRenderList list, int node, Direction flow) {
        queue.add(node, flow);

        var render = this.graph.getNode(node);

        if (!render.isEmpty()) {
            this.addChunkToVisible(list, render);
        }
    }

    public RenderSection getRenderSection(int x, int y, int z) {
        return this.graph.getNode(x, y, z);
    }

    public static float getDetailDistance(int renderDistance) {
        var detailDistance = SodiumClient.options().quality.detailDistance;

        if (detailDistance < 2) {
            // Automatic mode
            return Math.max(64.0f, (renderDistance - Math.max(2.0f, (renderDistance * 0.3f))) * 16.0f);
        } else if (detailDistance > 32) {
            // Maximum mode
            return 2048.0f;
        }

        // User-specified mode
        return detailDistance * 16.0f;
    }

    private int getTargetDetailLevel(RenderSection section) {
        if (section.getSquaredDistance(this.cameraX, this.cameraY, this.cameraZ) < this.detailFarPlane) {
            return ChunkDetailLevel.MAXIMUM_DETAIL;
        }

        return ChunkDetailLevel.MINIMUM_DETAIL;
    }

    public Collection<String> getDebugStrings() {
        int count = 0;

        long deviceUsed = 0;
        long deviceAllocated = 0;

        for (RenderRegion region : this.regions.getLoadedRegions()) {
            for (RenderRegionStorage storage : region.getAllStorage()) {
                deviceUsed += storage.getDeviceUsedMemory();
                deviceAllocated += storage.getDeviceAllocatedMemory();

                count++;
            }
        }

        List<String> list = new ArrayList<>();
        list.add(String.format("Chunk arena allocator: %s", SodiumClient.options().advanced.arenaMemoryAllocator.name()));
        list.add(String.format("Device buffer objects: %d", count));
        list.add(String.format("Device memory: %d/%d MiB", MathUtil.toMib(deviceUsed), MathUtil.toMib(deviceAllocated)));
        list.add(String.format("Staging buffer: %s", this.regions.getStagingBuffer().toString()));

        return list;
    }

    private void markChunkForUpdate(ChunkUpdateType type, RenderSection section) {
        this.updateQueue.add(section.getChunkX(), section.getChunkY(), section.getChunkZ(), type);
    }

    void onOcclusionDataUpdated(RenderSection section, ChunkOcclusionData occlusionData) {
        this.graph.setOcclusionData(section.id, occlusionData);
    }
}
