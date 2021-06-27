package me.jellysquid.mods.sodium.client.render.chunk;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.*;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.graph.ChunkGraphIterationQueue;
import me.jellysquid.mods.sodium.client.render.chunk.graph.ChunkGraphInfo;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPassManager;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionManager;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionVisibility;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderBuildTask;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderEmptyBuildTask;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderRebuildTask;
import me.jellysquid.mods.sodium.client.util.math.FrustumExtended;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListener;
import me.jellysquid.mods.sodium.client.world.ClientChunkManagerExtended;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSectionCache;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import me.jellysquid.mods.sodium.common.util.collections.FutureQueueDrainingIterator;
import me.jellysquid.mods.sodium.common.util.collections.QueueDrainingIterator;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.*;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;

public class RenderSectionManager implements ChunkStatusListener {
    /**
     * The maximum distance a chunk can be from the player's camera in order to be eligible for blocking updates.
     */
    private static final double NEARBY_CHUNK_DISTANCE = Math.pow(48, 2.0);

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
    private final ChunkRenderer chunkRenderer;

    private final RenderRegionManager regions;
    private final ClonedChunkSectionCache sectionCache;

    private final Long2ReferenceMap<RenderSection> sections = new Long2ReferenceOpenHashMap<>();

    private final ObjectArrayFIFOQueue<RenderSection> importantRebuildQueue = new ObjectArrayFIFOQueue<>();
    private final ObjectArrayFIFOQueue<RenderSection> rebuildQueue = new ObjectArrayFIFOQueue<>();

    private final Long2ReferenceMap<RenderChunkStatus> statusProcessingQueue = new Long2ReferenceLinkedOpenHashMap<>();
    private final ChunkAdjacencyMap adjacencyMap = new ChunkAdjacencyMap();

    private final Deque<ChunkBuildResult> uploadQueue = new ConcurrentLinkedDeque<>();

    private final ChunkRenderList chunkRenderList = new ChunkRenderList();
    private final ChunkGraphIterationQueue iterationQueue = new ChunkGraphIterationQueue();

    private final ObjectList<RenderSection> tickableChunks = new ObjectArrayList<>();
    private final ObjectList<BlockEntity> visibleBlockEntities = new ObjectArrayList<>();

    private final SodiumWorldRenderer worldRenderer;
    private final ClientWorld world;

    private final int renderDistance;

    private float cameraX, cameraY, cameraZ;
    private int centerChunkX, centerChunkY, centerChunkZ;

    private boolean isGraphDirty;

    private boolean useFogCulling;
    private boolean useOcclusionCulling;

    private double fogRenderCutoff;

    private FrustumExtended frustum;

    private int activeFrame = 0;

    public RenderSectionManager(SodiumWorldRenderer worldRenderer, ChunkRenderer chunkRenderer, BlockRenderPassManager renderPassManager, ClientWorld world, int renderDistance) {
        this.chunkRenderer = chunkRenderer;
        this.worldRenderer = worldRenderer;
        this.world = world;

        this.builder = new ChunkBuilder(chunkRenderer.getVertexType());
        this.builder.init(world, renderPassManager);

        this.isGraphDirty = true;
        this.renderDistance = renderDistance;

        this.regions = new RenderRegionManager(this.chunkRenderer);
        this.sectionCache = new ClonedChunkSectionCache(this.world);

        LongIterator it = ((ClientChunkManagerExtended) world.getChunkManager()).getLoadedChunks().iterator();

        while (it.hasNext()) {
            long pos = it.nextLong();

            this.onChunkAdded(ChunkPos.getPackedX(pos), ChunkPos.getPackedZ(pos));
        }
    }

    private void updateRegionVisibilities(FrustumExtended frustum) {
        for (RenderRegion region : this.regions.getLoadedRegions()) {
            region.updateVisibility(frustum);
        }
    }

    public void update(Camera camera, FrustumExtended frustum, int frame, boolean spectator) {
        this.resetLists();
        this.processStatusChanges();
        this.updateRegionVisibilities(frustum);

        this.setup(camera);
        this.iterateChunks(camera, frustum, frame, spectator);

        this.isGraphDirty = false;
    }

    private void processStatusChanges() {
        if (this.statusProcessingQueue.isEmpty())  {
            return;
        }

        for (Long2ReferenceMap.Entry<RenderChunkStatus> entry : this.statusProcessingQueue.long2ReferenceEntrySet()) {
            int x = ChunkPos.getPackedX(entry.getLongKey());
            int z = ChunkPos.getPackedZ(entry.getLongKey());

            for (int y = this.world.getBottomSectionCoord(); y < this.world.getTopSectionCoord(); y++) {
                this.isGraphDirty |= this.processStatusChangeForSection(x, y, z, entry.getValue());
            }

            if (entry.getValue() == RenderChunkStatus.LOAD) {
                this.adjacencyMap.onChunkLoaded(x, z);
            } else if (entry.getValue() == RenderChunkStatus.UNLOAD) {
                this.adjacencyMap.onChunkUnloaded(x, z);
            }
        }

        this.statusProcessingQueue.clear();
        this.isGraphDirty = true;
    }

    private boolean processStatusChangeForSection(int x, int y, int z, RenderChunkStatus status) {
        if (status == RenderChunkStatus.LOAD) {
            return this.loadSection(x, y, z);
        } else if (status == RenderChunkStatus.UNLOAD) {
            return this.unloadSection(x, y, z);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private void setup(Camera camera) {
        Vec3d cameraPos = camera.getPos();

        this.cameraX = (float) cameraPos.x;
        this.cameraY = (float) cameraPos.y;
        this.cameraZ = (float) cameraPos.z;

        this.useFogCulling = false;

        if (SodiumClientMod.options().advanced.useFogOcclusion) {
            float dist = RenderSystem.getShaderFogEnd() + FOG_PLANE_OFFSET;

            if (dist != 0.0f) {
                this.useFogCulling = true;
                this.fogRenderCutoff = Math.max(FOG_PLANE_MIN_DISTANCE, dist * dist);
            }
        }
    }

    private void iterateChunks(Camera camera, FrustumExtended frustum, int frame, boolean spectator) {
        this.initSearch(camera, frustum, frame, spectator);

        ChunkGraphIterationQueue queue = this.iterationQueue;

        for (int i = 0; i < queue.size(); i++) {
            RenderSection parent = queue.getRender(i);
            Direction flow = queue.getDirection(i);

            for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
                if (this.isCulled(parent.getGraphInfo(), flow, dir)) {
                    continue;
                }

                RenderSection adj = parent.getAdjacent(dir);

                if (adj != null && this.isWithinRenderDistance(adj)) {
                    this.bfsEnqueue(parent, adj, DirectionUtil.getOpposite(dir));
                }
            }
        }
    }

    private boolean canBuildChunk(RenderSection render) {
        return this.adjacencyMap.hasNeighbors(render.getChunkX(), render.getChunkZ());
    }

    private void addChunkToVisible(RenderSection render) {
        this.chunkRenderList.add(render);

        if (render.isTickable()) {
            this.tickableChunks.add(render);
        }
    }

    private void addEntitiesToRenderLists(RenderSection render) {
        Collection<BlockEntity> blockEntities = render.getData().getBlockEntities();

        if (!blockEntities.isEmpty()) {
            this.visibleBlockEntities.addAll(blockEntities);
        }
    }

    private void resetLists() {
        this.rebuildQueue.clear();
        this.importantRebuildQueue.clear();

        this.visibleBlockEntities.clear();
        this.chunkRenderList.clear();
        this.tickableChunks.clear();
    }

    public Collection<BlockEntity> getVisibleBlockEntities() {
        return this.visibleBlockEntities;
    }

    @Override
    public void onChunkAdded(int x, int z) {
        long key = ChunkPos.toLong(x, z);

        if (this.statusProcessingQueue.put(key, RenderChunkStatus.LOAD) == RenderChunkStatus.UNLOAD) {
            this.statusProcessingQueue.remove(key);
        }
    }

    @Override
    public void onChunkRemoved(int x, int z) {
        long key = ChunkPos.toLong(x, z);

        if (this.statusProcessingQueue.put(key, RenderChunkStatus.UNLOAD) == RenderChunkStatus.LOAD) {
            this.statusProcessingQueue.remove(key);
        }
    }

    private boolean loadSection(int x, int y, int z) {
        RenderRegion region = this.regions.createRegionForChunk(x, y, z);

        RenderSection render = new RenderSection(this.worldRenderer, x, y, z, region);
        region.addChunk(render);

        this.sections.put(ChunkSectionPos.asLong(x, y, z), render);

        Chunk chunk = this.world.getChunk(x, z);
        ChunkSection section = chunk.getSectionArray()[this.world.sectionCoordToIndex(y)];

        if (ChunkSection.isEmpty(section)) {
            render.setData(ChunkRenderData.EMPTY);
        } else {
            render.scheduleRebuild(false);
        }

        this.connectNeighborNodes(render);

        return true;
    }

    private boolean unloadSection(int x, int y, int z) {
        RenderSection chunk = this.sections.remove(ChunkSectionPos.asLong(x, y, z));

        if (chunk == null) {
            throw new IllegalStateException("Chunk is not loaded: " + ChunkSectionPos.asLong(x, y, z));
        }

        chunk.delete();

        this.disconnectNeighborNodes(chunk);

        RenderRegion region = chunk.getRegion();
        region.removeChunk(chunk);

        if (region.isEmpty()) {
            this.regions.unloadRegion(region);
        }

        return true;
    }

    public void renderLayer(MatrixStack matrixStack, BlockRenderPass pass, double x, double y, double z) {
        RenderDevice device = RenderDevice.INSTANCE;
        CommandList commandList = device.createCommandList();

        this.chunkRenderer.render(matrixStack, commandList, this.chunkRenderList, pass, new ChunkCameraContext(x, y, z));

        commandList.flush();
    }

    public void tickVisibleRenders() {
        for (RenderSection render : this.tickableChunks) {
            render.tick();
        }
    }

    public boolean isSectionVisible(int x, int y, int z) {
        RenderSection render = this.getRenderSection(x, y, z);

        if (render == null) {
            return false;
        }

        return render.getGraphInfo()
                .getLastVisibleFrame() == this.activeFrame;
    }

    public void updateChunks() {
        ArrayDeque<CompletableFuture<ChunkBuildResult>> blockingFutures = new ArrayDeque<>();

        int budget = this.builder.getSchedulingBudget();
        int submitted = 0;

        while (!this.importantRebuildQueue.isEmpty()) {
            RenderSection render = this.importantRebuildQueue.dequeue();

            // Do not allow distant chunks to block rendering
            if (!this.isChunkPrioritized(render)) {
                this.deferChunkRebuild(render);
            } else {
                blockingFutures.add(this.deferChunkRebuild(render));
            }

            this.isGraphDirty = true;
            submitted++;
        }

        while (submitted < budget && !this.rebuildQueue.isEmpty()) {
            RenderSection render = this.rebuildQueue.dequeue();

            this.deferChunkRebuild(render);
            submitted++;
        }

        this.isGraphDirty |= submitted > 0;

        // Try to complete some other work on the main thread while we wait for rebuilds to complete
        this.isGraphDirty |= this.performPendingUploads();

        if (!blockingFutures.isEmpty()) {
            this.regions.upload(RenderDevice.INSTANCE.createCommandList(), new FutureQueueDrainingIterator<>(blockingFutures));
        }
    }

    private boolean performPendingUploads() {
        this.uploadQueue.removeIf(result ->
                result.render.isDisposed());

        if (this.uploadQueue.isEmpty()) {
            return false;
        }

        this.regions.upload(RenderDevice.INSTANCE.createCommandList(), new QueueDrainingIterator<>(this.uploadQueue));

        return true;
    }

    private CompletableFuture<ChunkBuildResult> deferChunkRebuild(RenderSection render) {
        ChunkRenderBuildTask task = this.createRebuildTask(render);

        CompletableFuture<ChunkBuildResult> future = this.builder.schedule(task);
        future.thenAccept(this.uploadQueue::add);

        render.setRebuildFuture(future);

        return future;
    }

    public ChunkRenderBuildTask createRebuildTask(RenderSection render) {
        if (render.isDisposed()) {
            throw new IllegalStateException("Tried to rebuild a chunk " + render + " but it has been disposed");
        }

        ChunkRenderContext context = WorldSlice.prepare(this.world, render.getChunkPos(), this.sectionCache);

        if (context == null) {
            return new ChunkRenderEmptyBuildTask(render);
        }

        return new ChunkRenderRebuildTask(render, context);
    }

    public void markGraphDirty() {
        this.isGraphDirty = true;
    }

    public boolean isGraphDirty() {
        return this.isGraphDirty;
    }

    public ChunkBuilder getBuilder() {
        return this.builder;
    }

    public void destroy() {
        this.resetLists();

        this.regions.delete();
        this.builder.stopWorkers();
    }

    public int getTotalSections() {
        return this.regions.getLoadedRegions()
                .stream()
                .mapToInt(RenderRegion::getChunkCount)
                .sum();
    }

    public int getVisibleChunkCount() {
        return this.chunkRenderList.getCount();
    }

    public void scheduleRebuild(int x, int y, int z, boolean important) {
        RenderSection render = this.sections.get(ChunkSectionPos.asLong(x, y, z));

        if (render != null) {
            // Nearby chunks are always rendered immediately
            important = important || this.isChunkPrioritized(render);

            // Only enqueue chunks for updates if they aren't already enqueued for an update
            //
            // We should avoid rebuilding chunks that aren't visible by using data from the occlusion culler, however
            // that is not currently feasible because occlusion culling data is only ever updated when chunks are
            // rebuilt. Computation of occlusion data needs to be isolated from chunk rebuilds for that to be feasible.
            //
            // TODO: Avoid rebuilding chunks that aren't visible to the player
            if (render.scheduleRebuild(important)) {
                (render.needsImportantRebuild() ? this.importantRebuildQueue : this.rebuildQueue)
                        .enqueue(render);
            }

            this.isGraphDirty = true;
        }

        this.sectionCache.invalidate(x, y, z);
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

    private boolean isCulled(ChunkGraphInfo node, Direction from, Direction to) {
        if (node.canCull(to)) {
            return true;
        }

        return this.useOcclusionCulling && from != null && !node.isVisibleThrough(from, to);
    }

    private void initSearch(Camera camera, FrustumExtended frustum, int frame, boolean spectator) {
        this.activeFrame = frame;
        this.frustum = frustum;
        this.useOcclusionCulling = MinecraftClient.getInstance().chunkCullingEnabled;

        this.iterationQueue.clear();

        BlockPos origin = camera.getBlockPos();

        int chunkX = origin.getX() >> 4;
        int chunkY = origin.getY() >> 4;
        int chunkZ = origin.getZ() >> 4;

        this.centerChunkX = chunkX;
        this.centerChunkY = chunkY;
        this.centerChunkZ = chunkZ;

        RenderSection rootRender = this.getRenderSection(chunkX, chunkY, chunkZ);

        if (rootRender != null) {
            ChunkGraphInfo rootInfo = rootRender.getGraphInfo();
            rootInfo.resetCullingState();
            rootInfo.setLastVisibleFrame(frame);

            if (spectator && this.world.getBlockState(origin).isOpaqueFullCube(this.world, origin)) {
                this.useOcclusionCulling = false;
            }

            this.addVisible(rootRender, null);
        } else {
            chunkY = MathHelper.clamp(origin.getY() >> 4, this.world.getBottomSectionCoord(), this.world.getTopSectionCoord() - 1);

            List<RenderSection> sorted = new ArrayList<>();

            for (int x2 = -this.renderDistance; x2 <= this.renderDistance; ++x2) {
                for (int z2 = -this.renderDistance; z2 <= this.renderDistance; ++z2) {
                    RenderSection render = this.getRenderSection(chunkX + x2, chunkY, chunkZ + z2);

                    if (render == null) {
                        continue;
                    }

                    ChunkGraphInfo info = render.getGraphInfo();

                    if (info.isCulledByFrustum(frustum)) {
                        continue;
                    }

                    info.resetCullingState();
                    info.setLastVisibleFrame(frame);

                    sorted.add(render);
                }
            }

            sorted.sort(Comparator.comparingDouble(node -> node.getSquaredDistance(origin)));

            for (RenderSection render : sorted) {
                this.addVisible(render, null);
            }
        }
    }


    private void bfsEnqueue(RenderSection parent, RenderSection render, Direction flow) {
        ChunkGraphInfo info = render.getGraphInfo();

        if (info.getLastVisibleFrame() == this.activeFrame) {
            return;
        }

        RenderRegionVisibility parentVisibility = parent.getRegion().getVisibility();

        if (parentVisibility == RenderRegionVisibility.CULLED) {
            return;
        } else if (parentVisibility == RenderRegionVisibility.VISIBLE && info.isCulledByFrustum(this.frustum)) {
            return;
        }

        info.setLastVisibleFrame(this.activeFrame);
        info.setCullingState(parent.getGraphInfo().getCullingState(), flow);

        this.addVisible(render, flow);
    }

    private void addVisible(RenderSection render, Direction flow) {
        this.iterationQueue.add(render, flow);

        if (render.needsRebuild() && this.canBuildChunk(render)) {
            if (render.needsImportantRebuild()) {
                this.importantRebuildQueue.enqueue(render);
            } else {
                this.rebuildQueue.enqueue(render);
            }
        }

        if (this.useFogCulling && render.getSquaredDistanceXZ(this.cameraX, this.cameraZ) >= this.fogRenderCutoff) {
            return;
        }

        if (!render.isEmpty()) {
            this.addChunkToVisible(render);
            this.addEntitiesToRenderLists(render);
        }
    }

    private void connectNeighborNodes(RenderSection render) {
        for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
            RenderSection adj = this.getRenderSection(render.getChunkX() + dir.getOffsetX(),
                    render.getChunkY() + dir.getOffsetY(),
                    render.getChunkZ() + dir.getOffsetZ());

            if (adj != null) {
                adj.setAdjacentNode(DirectionUtil.getOpposite(dir), render);
                render.setAdjacentNode(dir, adj);
            }
        }
    }

    private void disconnectNeighborNodes(RenderSection render) {
        for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
            RenderSection adj = render.getAdjacent(dir);

            if (adj != null) {
                adj.setAdjacentNode(DirectionUtil.getOpposite(dir), null);
                render.setAdjacentNode(dir, null);
            }
        }
    }

    private RenderSection getRenderSection(int x, int y, int z) {
        return this.sections.get(ChunkSectionPos.asLong(x, y, z));
    }

    enum RenderChunkStatus {
        LOAD,
        UNLOAD
    }
}
