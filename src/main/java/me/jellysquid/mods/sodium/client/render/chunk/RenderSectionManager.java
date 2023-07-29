package me.jellysquid.mods.sodium.client.render.chunk;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterable;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import me.jellysquid.mods.sodium.client.render.chunk.graph.GraphDirection;
import me.jellysquid.mods.sodium.client.render.chunk.graph.VisibilityEncoding;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.lists.SortedRenderListBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.lists.SortedRenderLists;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionManager;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderBuildTask;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderEmptyBuildTask;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderRebuildTask;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkMeshFormats;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import me.jellysquid.mods.sodium.client.util.BitwiseMath;
import me.jellysquid.mods.sodium.client.util.MathUtil;
import me.jellysquid.mods.sodium.client.util.collections.WorkStealingFutureDrain;
import me.jellysquid.mods.sodium.client.util.sorting.MergeSort;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSectionCache;
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

    private final ChunkBuilder builder;

    private final RenderRegionManager regions;
    private final ClonedChunkSectionCache sectionCache;

    private final Long2ReferenceMap<RenderSection> sectionByPosition = new Long2ReferenceOpenHashMap<>();

    private final Map<ChunkUpdateType, PriorityQueue<RenderSection>> rebuildQueues = new EnumMap<>(ChunkUpdateType.class);

    private final ArrayDeque<RenderSection> iterationQueue = new ArrayDeque<>();

    private final ChunkRenderer chunkRenderer;

    private final SodiumWorldRenderer worldRenderer;
    private final ClientWorld world;

    private final int renderDistance;
    private int effectiveRenderDistance;

    private float cameraX, cameraY, cameraZ;
    private int centerChunkX, centerChunkY, centerChunkZ;

    private boolean needsUpdate;

    private boolean useOcclusionCulling;
    private boolean useBlockFaceCulling;

    private Viewport viewport;

    private int currentFrame = 0;
    private boolean alwaysDeferChunkUpdates;

    private final SortedRenderListBuilder renderListBuilder = new SortedRenderListBuilder();
    private SortedRenderLists renderLists;

    public RenderSectionManager(SodiumWorldRenderer worldRenderer, ClientWorld world, int renderDistance, CommandList commandList) {
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
    }

    public void update(Camera camera, Viewport viewport, int frame, boolean spectator) {
        this.resetLists();

        var renderList = this.renderListBuilder;
        renderList.reset();

        this.setup(camera);
        this.iterateChunks(renderList, camera, viewport, frame, spectator);

        this.renderLists = renderList.build();
        this.needsUpdate = false;
    }

    private void setup(Camera camera) {
        Vec3d cameraPos = camera.getPos();

        this.cameraX = (float) cameraPos.x;
        this.cameraY = (float) cameraPos.y;
        this.cameraZ = (float) cameraPos.z;

        var options = SodiumClientMod.options();

        this.alwaysDeferChunkUpdates = options.performance.alwaysDeferChunkUpdates;
    }

    private void iterateChunks(SortedRenderListBuilder renderLists, Camera camera, Viewport viewport, int frame, boolean spectator) {
        this.initSearch(camera, viewport, frame, spectator);

        while (!this.iterationQueue.isEmpty()) {
            RenderSection section = this.iterationQueue.remove();

            if (!this.isWithinRenderDistance(section) || this.isOutsideViewport(section, this.viewport)) {
                continue;
            }

            this.addToRenderLists(renderLists, section);

            if (section.getPendingUpdate() != null) {
                this.schedulePendingUpdates(section);
            }

            int connections;

            if (this.useOcclusionCulling) {
                connections = VisibilityEncoding.getConnections(section.getVisibilityData(), section.getIncomingDirections());
            } else {
                connections = GraphDirection.ALL;
            }

            connections &= this.getOutwardDirections(section.getChunkX(), section.getChunkY(), section.getChunkZ());

            if (connections != GraphDirection.NONE) {
                this.searchNeighbors(section, connections);
            }
        }
    }

    private void addToRenderLists(SortedRenderListBuilder renderLists, RenderSection section) {
        renderLists.add(section, this.getVisibleFaces(section.getChunkX(), section.getChunkY(), section.getChunkZ()));
    }

    private static final int MODEL_UNASSIGNED = ModelQuadFacing.UNASSIGNED.ordinal();
    private static final int MODEL_POS_X      = ModelQuadFacing.POS_X.ordinal();
    private static final int MODEL_NEG_X      = ModelQuadFacing.NEG_X.ordinal();
    private static final int MODEL_POS_Y      = ModelQuadFacing.POS_Y.ordinal();
    private static final int MODEL_NEG_Y      = ModelQuadFacing.NEG_Y.ordinal();
    private static final int MODEL_POS_Z      = ModelQuadFacing.POS_Z.ordinal();
    private static final int MODEL_NEG_Z      = ModelQuadFacing.NEG_Z.ordinal();

    private int getVisibleFaces(int x, int y, int z) {
        if (!this.useBlockFaceCulling) {
            return ModelQuadFacing.ALL;
        }

        // This is carefully written so that we can keep everything branch-less.
        //
        // Normally, this would be a ridiculous way to handle the problem. But the Hotspot VM's
        // heuristic for generating SETcc/CMOV instructions is broken, and it will always create a
        // branch even when a trivial ternary is encountered.
        //
        // For example, the following will never be transformed into a SETcc:
        //   (a > b) ? 1 : 0
        //
        // So we have to instead rely on sign-bit extension and masking (which generates a ton
        // of unnecessary instructions) to get this to be branch-less.
        //
        // To do this, we can transform the previous expression into the following.
        //   (b - a) >> 31
        //
        // This works because if (a > b) then (b - a) will always create a negative number. We then shift the sign bit
        // into the least significant bit's position (which also discards any bits following the sign bit) to get the
        // output we are looking for.
        //
        // If you look at the output which LLVM produces for a series of ternaries, you will instantly become distraught,
        // because it manages to a) correctly evaluate the cost of instructions, and b) go so far
        // as to actually produce vector code.  (https://godbolt.org/z/GaaEx39T9)

        int planes = 0;

        planes |= BitwiseMath.lessThan(x - 1, this.centerChunkX) << MODEL_POS_X;
        planes |= BitwiseMath.lessThan(y - 1, this.centerChunkY) << MODEL_POS_Y;
        planes |= BitwiseMath.lessThan(z - 1, this.centerChunkZ) << MODEL_POS_Z;

        planes |= BitwiseMath.greaterThan(x + 1, this.centerChunkX) << MODEL_NEG_X;
        planes |= BitwiseMath.greaterThan(y + 1, this.centerChunkY) << MODEL_NEG_Y;
        planes |= BitwiseMath.greaterThan(z + 1, this.centerChunkZ) << MODEL_NEG_Z;

        // the "unassigned" plane is always front-facing, since we can't check it
        planes |= (1 << MODEL_UNASSIGNED);

        return planes;
    }

    private void searchNeighbors(RenderSection section, int outgoing) {
        for (int direction = 0; direction < GraphDirection.COUNT; direction++) {
            if ((outgoing & (1 << direction)) == 0) {
                continue;
            }

            RenderSection adj = section.getAdjacent(direction);

            if (adj != null) {
                this.bfsEnqueue(adj, 1 << GraphDirection.opposite(direction));
            }
        }
    }

    private int getOutwardDirections(int x, int y, int z) {
        int planes = 0;

        planes |= x <= this.centerChunkX ? 1 << GraphDirection.WEST  : 0;
        planes |= x >= this.centerChunkX ? 1 << GraphDirection.EAST  : 0;

        planes |= y <= this.centerChunkY ? 1 << GraphDirection.DOWN  : 0;
        planes |= y >= this.centerChunkY ? 1 << GraphDirection.UP    : 0;

        planes |= z <= this.centerChunkZ ? 1 << GraphDirection.NORTH : 0;
        planes |= z >= this.centerChunkZ ? 1 << GraphDirection.SOUTH : 0;

        return planes;
    }

    private static final float CHUNK_RENDER_BOUNDS_EPSILON = 0.0125f;

    private boolean isOutsideViewport(RenderSection section, Viewport viewport) {
        float x = section.getOriginX();
        float y = section.getOriginY();
        float z = section.getOriginZ();

        // TODO: This epsilon is a hack because we're losing precision in the world->view transform
        // We should first translate chunk coordinates to camera-relative space, and then create a bounding
        // box from that.
        float minX = x - CHUNK_RENDER_BOUNDS_EPSILON;
        float minY = y - CHUNK_RENDER_BOUNDS_EPSILON;
        float minZ = z - CHUNK_RENDER_BOUNDS_EPSILON;

        float maxX = x + 16.0f + CHUNK_RENDER_BOUNDS_EPSILON;
        float maxY = y + 16.0f + CHUNK_RENDER_BOUNDS_EPSILON;
        float maxZ = z + 16.0f + CHUNK_RENDER_BOUNDS_EPSILON;

        return !viewport.isBoxVisible(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private void schedulePendingUpdates(RenderSection section) {
        PriorityQueue<RenderSection> queue = this.rebuildQueues.get(section.getPendingUpdate());

        if (queue.size() >= 32) {
            return;
        }

        queue.enqueue(section);
    }

    private void resetLists() {
        for (PriorityQueue<RenderSection> queue : this.rebuildQueues.values()) {
            queue.clear();
        }

        this.renderLists = null;
    }

    public void onSectionAdded(int x, int y, int z) {
        long key = ChunkSectionPos.asLong(x, y, z);

        if (this.sectionByPosition.containsKey(key)) {
            return;
        }

        RenderRegion region = this.regions.createForChunk(x, y, z);

        RenderSection renderSection = new RenderSection(region, this.worldRenderer, x, y, z);
        region.addSection(renderSection);

        this.sectionByPosition.put(key, renderSection);

        Chunk chunk = this.world.getChunk(x, z);
        ChunkSection section = chunk.getSectionArray()[this.world.sectionCoordToIndex(y)];

        if (section.isEmpty()) {
            renderSection.setData(BuiltSectionInfo.EMPTY);
        } else {
            renderSection.markForUpdate(ChunkUpdateType.INITIAL_BUILD);
        }

        this.connectNeighborNodes(renderSection);

        this.needsUpdate = true;
    }

    public void onSectionRemoved(int x, int y, int z) {
        RenderSection section = this.sectionByPosition.remove(ChunkSectionPos.asLong(x, y, z));

        if (section == null) {
            return;
        }

        RenderRegion region = section.getRegion();

        if (region != null) {
            region.removeSection(section);
        }

        this.disconnectNeighborNodes(section);

        section.delete();

        this.needsUpdate = true;
    }

    public void renderLayer(ChunkRenderMatrices matrices, TerrainRenderPass pass, double x, double y, double z) {
        Validate.notNull(this.renderLists, "Render list is null");

        RenderDevice device = RenderDevice.INSTANCE;
        CommandList commandList = device.createCommandList();

        this.chunkRenderer.render(matrices, commandList, this.regions, this.renderLists, pass, new ChunkCameraContext(x, y, z));

        commandList.flush();
    }

    public void tickVisibleRenders() {
        Iterator<ChunkRenderList> it = this.renderLists.sorted();

        while (it.hasNext()) {
            ChunkRenderList renderList = it.next();

            var region = renderList.getRegion();
            var iterator = renderList.sectionsWithSpritesIterator();

            if (iterator == null) {
                continue;
            }

            while (iterator.hasNext()) {
                var section = region.getSection(iterator.next());
                section.tick();
            }
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

        this.regions.update();
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
        return this.needsUpdate;
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

        this.builder.stopWorkers();
    }

    public int getTotalSections() {
        return this.sectionByPosition.size();
    }

    public int getVisibleChunkCount() {
        var sections = 0;
        var iterator = this.renderLists.sorted();

        while (iterator.hasNext()) {
            var renderList = iterator.next();
            sections += renderList.getSectionsWithGeometryCount();
        }

        return sections;
    }

    public void scheduleRebuild(int x, int y, int z, boolean important) {
        this.sectionCache.invalidate(x, y, z);

        RenderSection section = this.sectionByPosition.get(ChunkSectionPos.asLong(x, y, z));

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

    public void onChunkRenderUpdates(int x, int y, int z, BuiltSectionInfo data) {
        RenderSection node = this.getRenderSection(x, y, z);

        if (node != null) {
            node.setOcclusionData(data.getOcclusionData());
        }
    }

    private boolean isWithinRenderDistance(RenderSection section) {
        int x = Math.abs(section.getChunkX() - this.centerChunkX);
        int y = Math.abs(section.getChunkY() - this.centerChunkY);
        int z = Math.abs(section.getChunkZ() - this.centerChunkZ);

        return Math.max(x, Math.max(y, z)) <= this.effectiveRenderDistance;
    }

    private void initSearch(Camera camera, Viewport viewport, int frame, boolean spectator) {
        this.iterationQueue.clear();

        this.currentFrame = frame;
        this.viewport = viewport;

        var options = SodiumClientMod.options();

        if (options.performance.useFogOcclusion) {
            this.effectiveRenderDistance = Math.min(this.getEffectiveViewDistance(), this.renderDistance);
        } else {
            this.effectiveRenderDistance = this.renderDistance;
        }

        this.useBlockFaceCulling = options.performance.useBlockFaceCulling;

        BlockPos origin = camera.getBlockPos();

        if (spectator && this.world.getBlockState(origin).isOpaqueFullCube(this.world, origin)) {
            this.useOcclusionCulling = false;
        } else {
            this.useOcclusionCulling = MinecraftClient.getInstance().chunkCullingEnabled;
        }

        this.centerChunkX = origin.getX() >> 4;
        this.centerChunkY = origin.getY() >> 4;
        this.centerChunkZ = origin.getZ() >> 4;

        if (this.centerChunkY < this.world.getBottomSectionCoord()) {
            this.initSearchFallback(viewport, origin, this.centerChunkX, this.world.getBottomSectionCoord(), this.centerChunkZ, 1 << GraphDirection.DOWN);
        } else if (this.centerChunkY >= this.world.getTopSectionCoord()) {
            this.initSearchFallback(viewport, origin, this.centerChunkX, this.world.getTopSectionCoord() - 1, this.centerChunkZ, 1 << GraphDirection.UP);
        } else {
            var node = this.getRenderSection(this.centerChunkX, this.centerChunkY, this.centerChunkZ);

            if (node != null) {
                this.bfsEnqueue(node, GraphDirection.ALL);
            }
        }
    }

    private void initSearchFallback(Viewport viewport, BlockPos origin, int chunkX, int chunkY, int chunkZ, int directions) {
        List<RenderSection> sections = new ArrayList<>();

        for (int x2 = -this.effectiveRenderDistance; x2 <= this.effectiveRenderDistance; ++x2) {
            for (int z2 = -this.effectiveRenderDistance; z2 <= this.effectiveRenderDistance; ++z2) {
                RenderSection section = this.getRenderSection(chunkX + x2, chunkY, chunkZ + z2);

                if (section == null || this.isOutsideViewport(section, viewport)) {
                    continue;
                }

                sections.add(section);
            }
        }

        if (!sections.isEmpty()) {
            this.bfsEnqueueAll(sections, origin, directions);
        }
    }

    private void bfsEnqueueAll(List<RenderSection> sections, BlockPos origin, int directions) {
        final var distance = new float[sections.size()];

        for (int index = 0; index < sections.size(); index++) {
            var section = sections.get(index);
            distance[index] = -section.getSquaredDistance(origin); // sort by closest to camera
        }

        // TODO: avoid indirect sort via indices
        for (int index : MergeSort.mergeSort(distance)) {
            this.bfsEnqueue(sections.get(index), directions);
        }
    }

    private void bfsEnqueue(RenderSection render, int incomingDirections) {
        if (render.getLastVisibleFrame() != this.currentFrame) {
            render.setLastVisibleFrame(this.currentFrame);
            render.setIncomingDirections(GraphDirection.NONE);

            this.iterationQueue.add(render);
        }

        render.addIncomingDirections(incomingDirections);
    }

    private int getEffectiveViewDistance() {
        var color = RenderSystem.getShaderFogColor();
        var distance = RenderSystem.getShaderFogEnd();

        // The fog must be fully opaque in order to skip rendering of chunks behind it
        if (!MathHelper.approximatelyEquals(color[3], 1.0f)) {
            return this.renderDistance;
        }

        return MathHelper.floor(distance) >> 4;
    }

    private void connectNeighborNodes(RenderSection render) {
        for (int direction = 0; direction < GraphDirection.COUNT; direction++) {
            RenderSection adj = this.getRenderSection(render.getChunkX() + GraphDirection.x(direction),
                    render.getChunkY() + GraphDirection.y(direction),
                    render.getChunkZ() + GraphDirection.z(direction));

            if (adj != null) {
                adj.setAdjacentNode(GraphDirection.opposite(direction), render);
                render.setAdjacentNode(direction, adj);
            }
        }
    }

    private void disconnectNeighborNodes(RenderSection render) {
        for (int direction = 0; direction < GraphDirection.COUNT; direction++) {
            RenderSection adj = render.getAdjacent(direction);

            if (adj != null) {
                adj.setAdjacentNode(GraphDirection.opposite(direction), null);
                render.setAdjacentNode(direction, null);
            }
        }
    }

    private RenderSection getRenderSection(int x, int y, int z) {
        return this.sectionByPosition.get(ChunkSectionPos.asLong(x, y, z));
    }

    public Collection<String> getDebugStrings() {
        List<String> list = new ArrayList<>();

        int count = 0;

        long deviceUsed = 0;
        long deviceAllocated = 0;

        for (var region : this.regions.getLoadedRegions()) {
            var resources = region.getResources();

            if (resources == null) {
                continue;
            }

            var buffer = resources.getGeometryArena();

            deviceUsed += buffer.getDeviceUsedMemory();
            deviceAllocated += buffer.getDeviceAllocatedMemory();

            count++;
        }

        list.add(String.format("Device buffer objects: %d", count));
        list.add(String.format("Device memory: %d/%d MiB", MathUtil.toMib(deviceUsed), MathUtil.toMib(deviceAllocated)));
        list.add(String.format("Staging buffer: %s", this.regions.getStagingBuffer().toString()));
        return list;
    }

    public SortedRenderLists getRenderLists() {
        return this.renderLists;
    }

    public boolean isSectionBuilt(int x, int y, int z) {
        var section = this.getRenderSection(x, y, z);
        return section != null && section.isBuilt();
    }

    public void onChunkAdded(int x, int z) {
        for (int y = this.world.getBottomSectionCoord(); y < this.world.getTopSectionCoord(); y++) {
            this.onSectionAdded(x, y, z);
        }
    }

    public void onChunkRemoved(int x, int z) {
        for (int y = this.world.getBottomSectionCoord(); y < this.world.getTopSectionCoord(); y++) {
            this.onSectionRemoved(x, y, z);
        }
    }
}
