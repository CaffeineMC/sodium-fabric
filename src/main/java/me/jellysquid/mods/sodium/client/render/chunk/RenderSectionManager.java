package me.jellysquid.mods.sodium.client.render.chunk;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkModelVertexFormats;
import me.jellysquid.mods.sodium.client.render.chunk.graph.ChunkGraphIterationQueue;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPassManager;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionManager;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderBuildTask;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderEmptyBuildTask;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderRebuildTask;
import me.jellysquid.mods.sodium.client.util.MathUtil;
import me.jellysquid.mods.sodium.client.util.collections.BitArray;
import me.jellysquid.mods.sodium.client.util.frustum.Frustum;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSectionCache;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import me.jellysquid.mods.sodium.common.util.collections.WorkStealingFutureDrain;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import org.joml.Vector3f;

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
    private final Map<ChunkUpdateType, PriorityQueue<RenderSection>> rebuildQueues = new EnumMap<>(ChunkUpdateType.class);

    private final ChunkRenderList chunkRenderList = new ChunkRenderList();
    private final ChunkGraphIterationQueue iterationQueue = new ChunkGraphIterationQueue();

    private final IntArrayList tickableChunks = new IntArrayList();
    private final IntArrayList entityChunks = new IntArrayList();

    private final RegionChunkRenderer chunkRenderer;

    private final SodiumWorldRenderer worldRenderer;
    private final ClientWorld world;

    private final int renderDistance;
    private final int bottomSectionCoord, topSectionCoord;

    private float cameraX, cameraY, cameraZ;
    private int centerChunkX, centerChunkY, centerChunkZ;

    private boolean needsUpdate;

    private boolean useFogCulling;
    private boolean useOcclusionCulling;

    private float fogRenderCutoff;

    private Frustum frustum;

    private int currentFrame = 0;
    private boolean alwaysDeferChunkUpdates;

    private final ChunkTracker tracker;

    private static class State {
        private static final long DEFAULT_VISIBILITY_DATA = calculateVisibilityData(ChunkRenderData.EMPTY.getOcclusionData());

        private final int widthShift;
        private final int sectionWidthMask;
        private final int sectionHeightMask;
        private final int heightShift;
        private final BitArray queuedChunks;
        private final RenderSection[] sections;
        private final long[] visibilityData;
        private final byte[] cullingState;

        public State(World world, int renderDistance) {
            int sectionHeight = MathHelper.smallestEncompassingPowerOfTwo(world.getTopSectionCoord() - world.getBottomSectionCoord());
            this.sectionHeightMask = sectionHeight - 1;
            this.heightShift = Integer.numberOfTrailingZeros(sectionHeight);

            int sectionWidth = MathHelper.smallestEncompassingPowerOfTwo((renderDistance * 2) + 1);
            this.sectionWidthMask = sectionWidth - 1;
            this.widthShift = Integer.numberOfTrailingZeros(sectionWidth);

            int arraySize = sectionWidth * sectionHeight * sectionWidth;

            this.queuedChunks = new BitArray(arraySize);
            this.sections = new RenderSection[arraySize];
            this.visibilityData = new long[arraySize];
            this.cullingState = new byte[arraySize];

            Arrays.fill(this.visibilityData, DEFAULT_VISIBILITY_DATA);
        }

        public void reset() {
            this.queuedChunks.clear();

            Arrays.fill(this.cullingState, (byte) 0);
        }

        public int getSectionId(int x, int y, int z) {
            return ((x & this.sectionWidthMask) << (this.widthShift + this.heightShift)) | ((z & this.sectionWidthMask) << (this.heightShift)) | (y & this.sectionHeightMask);
        }

        public void setVisible(int id) {
            this.queuedChunks.set(id);
        }

        public boolean isVisible(int id) {
            return this.queuedChunks.get(id);
        }

        public RenderSection getSection(int id) {
            return this.sections[id];
        }
        public void setSection(int id, RenderSection section) {
            this.sections[id] = section;
            this.visibilityData[id] = DEFAULT_VISIBILITY_DATA;
        }

        public void setOcclusionData(ChunkOcclusionData occlusionData, int id) {
            this.visibilityData[id] = calculateVisibilityData(occlusionData);
        }

        private static long calculateVisibilityData(ChunkOcclusionData occlusionData) {
            long visibilityData = 0;

            for (Direction from : DirectionUtil.ALL_DIRECTIONS) {
                for (Direction to : DirectionUtil.ALL_DIRECTIONS) {
                    if (occlusionData == null || occlusionData.isVisibleThrough(from, to)) {
                        visibilityData |= (1L << ((from.ordinal() << 3) + to.ordinal()));
                    }
                }
            }

            return visibilityData;
        }

        public boolean isVisibleThrough(int id, int from, int to) {
            return ((this.visibilityData[id] & (1L << ((from << 3) + to))) != 0L);
        }

        public void setCullingState(int id, byte parent, int dir) {
            this.cullingState[id] = (byte) (parent | (1 << dir));
        }

        public boolean canCull(int id, int dir) {
            return (this.cullingState[id] & 1 << dir) != 0;
        }

        public byte getCullingState(int id) {
            return this.cullingState[id];
        }

        public boolean isLoaded(int id) {
            return this.sections[id] != null;
        }

        public RenderSection removeSection(int id) {
            var section = this.sections[id];
            this.sections[id] = null;

            return section;
        }
    }

    private final State state;

    public RenderSectionManager(SodiumWorldRenderer worldRenderer, BlockRenderPassManager renderPassManager, ClientWorld world, int renderDistance, CommandList commandList) {
        this.chunkRenderer = new RegionChunkRenderer(RenderDevice.INSTANCE, ChunkModelVertexFormats.DEFAULT);

        this.worldRenderer = worldRenderer;
        this.world = world;

        this.builder = new ChunkBuilder(ChunkModelVertexFormats.DEFAULT);
        this.builder.init(world, renderPassManager);

        this.needsUpdate = true;
        this.renderDistance = renderDistance;

        this.regions = new RenderRegionManager(commandList);
        this.sectionCache = new ClonedChunkSectionCache(this.world);

        for (ChunkUpdateType type : ChunkUpdateType.values()) {
            this.rebuildQueues.put(type, new ObjectArrayFIFOQueue<>());
        }

        this.tracker = this.worldRenderer.getChunkTracker();

        this.bottomSectionCoord = this.world.getBottomSectionCoord();
        this.topSectionCoord = this.world.getTopSectionCoord();

        this.state = new State(this.world, renderDistance);
    }

    public void reloadChunks(ChunkTracker tracker) {
        tracker.getChunks(ChunkStatus.FLAG_HAS_BLOCK_DATA)
                .forEach(pos -> this.onChunkAdded(ChunkPos.getPackedX(pos), ChunkPos.getPackedZ(pos)));

        tracker.getChunks(ChunkStatus.FLAG_ALL)
                .forEach(pos -> this.scheduleReadyChunk(ChunkPos.getPackedX(pos), ChunkPos.getPackedZ(pos)));
    }

    public void update(Camera camera, Frustum frustum, int frame, boolean spectator) {
        this.resetLists();

        this.setup(camera);
        this.iterateChunks(camera, frustum, frame, spectator);

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

        this.state.reset();
    }

    private void iterateChunks(Camera camera, Frustum frustum, int frame, boolean spectator) {
        this.initSearch(camera, frustum, frame, spectator);

        ChunkGraphIterationQueue queue = this.iterationQueue;

        Vector3f cameraPos = new Vector3f((float) camera.getPos().x, (float) camera.getPos().y, (float) camera.getPos().z);
        Vector3f cameraSectionOrigin = new Vector3f(MathHelper.floor(cameraPos.x / 16.0) * 16, MathHelper.floor(cameraPos.y / 16.0) * 16, MathHelper.floor(cameraPos.z / 16.0) * 16);

        boolean isRayCullEnabled = SodiumClientMod.options().performance.useRasterOcclusionCulling;

        for (int i = 0; i < queue.size(); i++) {
            var sectionId = queue.getSection(i);
            var incomingDirection = queue.getDirection(i);

            var section = this.state.getSection(sectionId);

            this.performScheduling(section);

            boolean doRayCull = isRayCullEnabled && (Math.abs(section.getOriginX() - cameraSectionOrigin.x) > 60 ||
                    Math.abs(section.getOriginY() - cameraSectionOrigin.y) > 60 ||
                    Math.abs(section.getOriginZ() - cameraSectionOrigin.z) > 60);

            for (var outgoingDirection : DirectionUtil.ALL_DIRECTIONS) {
                if (this.isCulled(sectionId, incomingDirection, outgoingDirection.ordinal())) {
                    continue;
                }

                int adjX = section.getChunkX() + outgoingDirection.getOffsetX();
                int adjY = section.getChunkY() + outgoingDirection.getOffsetY();
                int adjZ = section.getChunkZ() + outgoingDirection.getOffsetZ();

                if (this.isWithinRenderDistance(adjX, adjY, adjZ)) {
                    var adjId = this.state.getSectionId(adjX, adjY, adjZ);

                    if (this.state.isLoaded(adjId)) {
                        this.bfsEnqueue(sectionId, adjId, adjX, adjY, adjZ,
                                DirectionUtil.getOpposite(outgoingDirection).ordinal(), doRayCull);
                    }
                }
            }
        }
    }

    private void performScheduling(RenderSection section) {
        if (section.getPendingUpdate() != null) {
            var queue = this.rebuildQueues.get(section.getPendingUpdate());

            if (queue.size() >= 32) {
                return;
            }

            queue.enqueue(section);
        }
    }

    private void resetLists() {
        for (var queue : this.rebuildQueues.values()) {
            queue.clear();
        }

        this.entityChunks.clear();
        this.chunkRenderList.clear();
        this.tickableChunks.clear();
    }

    public Iterator<BlockEntity> getVisibleBlockEntities() {
        return this.entityChunks.intStream()
                .mapToObj(this.state::getSection)
                .flatMap(section -> section.getData()
                        .getBlockEntities()
                        .stream())
                .iterator();
    }

    public void onChunkAdded(int x, int z) {
        for (int y = this.world.getBottomSectionCoord(); y < this.world.getTopSectionCoord(); y++) {
            this.needsUpdate |= this.loadSection(x, y, z);
        }
    }

    public void onChunkRemoved(int x, int z) {
        for (int y = this.world.getBottomSectionCoord(); y < this.world.getTopSectionCoord(); y++) {
            this.needsUpdate |= this.unloadSection(x, y, z);
        }
    }

    private boolean loadSection(int x, int y, int z) {
        var id = this.state.getSectionId(x, y, z);

        if (this.state.isLoaded(id)) {
            throw new IllegalStateException("Chunk is already loaded: " + ChunkSectionPos.from(x, y, z));
        }

        RenderRegion region = this.regions.createRegionForChunk(x, y, z);

        RenderSection render = new RenderSection(this.worldRenderer, x, y, z, region);
        region.addChunk(render);

        this.state.setSection(id, render);

        return true;
    }

    private boolean unloadSection(int x, int y, int z) {
        var id = this.state.getSectionId(x, y, z);

        if (!this.state.isLoaded(id)) {
            throw new IllegalStateException("Chunk is not loaded: " + ChunkSectionPos.from(x, y, z));
        }

        RenderSection chunk = this.state.removeSection(id);

        chunk.delete();

        RenderRegion region = chunk.getRegion();
        region.removeChunk(chunk);

        this.readyColumns.remove(ChunkSectionPos.asLong(x, y, z));

        return true;
    }

    public void renderLayer(ChunkRenderMatrices matrices, BlockRenderPass pass, double x, double y, double z) {
        RenderDevice device = RenderDevice.INSTANCE;
        CommandList commandList = device.createCommandList();

        this.chunkRenderer.render(matrices, commandList, this.chunkRenderList, pass, new ChunkCameraContext(x, y, z));

        commandList.flush();
    }

    public void tickVisibleRenders() {
        IntIterator it = this.tickableChunks.iterator();

        while (it.hasNext()) {
            var id = it.nextInt();

            var section = this.state.getSection(id);
            section.tick();
        }
    }

    public boolean isSectionVisible(int x, int y, int z) {
        return this.state.isVisible(this.state.getSectionId(x, y, z));
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

        var sectionCache = new ClonedChunkSectionCache(this.world);

        this.submitRebuildTasks(ChunkUpdateType.IMPORTANT_REBUILD, blockingFutures, sectionCache);
        this.submitRebuildTasks(ChunkUpdateType.INITIAL_BUILD, allImmediately ? blockingFutures : null, sectionCache);
        this.submitRebuildTasks(ChunkUpdateType.REBUILD, allImmediately ? blockingFutures : null, sectionCache);

        // Try to complete some other work on the main thread while we wait for rebuilds to complete
        this.needsUpdate |= this.performPendingUploads();

        if (!blockingFutures.isEmpty()) {
            this.needsUpdate = true;
            this.regions.upload(RenderDevice.INSTANCE.createCommandList(), new WorkStealingFutureDrain<>(blockingFutures, this.builder::stealTask));
        }

        this.regions.cleanup();
    }

    private void submitRebuildTasks(ChunkUpdateType filterType, LinkedList<CompletableFuture<ChunkBuildResult>> immediateFutures, ClonedChunkSectionCache sectionCache) {
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

            ChunkRenderBuildTask task = this.createRebuildTask(sectionCache, section);
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

    public ChunkRenderBuildTask createRebuildTask(ClonedChunkSectionCache sectionCache, RenderSection render) {
        ChunkRenderContext context = WorldSlice.prepare(this.world, render.getChunkPos(), sectionCache);
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
        }

        this.chunkRenderer.delete();
        this.builder.stopWorkers();
    }

    public int getTotalSections() {
        int sum = 0;

        for (RenderRegion region : this.regions.getLoadedRegions()) {
            sum += region.getChunkCount();
        }

        return sum;
    }

    public int getVisibleChunkCount() {
        return this.chunkRenderList.getCount();
    }

    public void scheduleRebuild(int x, int y, int z, boolean important) {
        if (!this.canSubmitChunkForRebuilds(x, z)) {
            return;
        }

        RenderSection section = this.state.getSection(this.state.getSectionId(x, y, z));

        if (section != null && section.isBuilt()) {
            if (!this.alwaysDeferChunkUpdates && important) {
                section.markForUpdate(ChunkUpdateType.IMPORTANT_REBUILD);
            } else {
                section.markForUpdate(ChunkUpdateType.REBUILD);
            }
        }

        this.needsUpdate = true;
    }

    public void onChunkRenderUpdates(int x, int y, int z, ChunkRenderData data) {
        this.state.setOcclusionData(data.getOcclusionData(), this.state.getSectionId(x, y, z));
    }

    private boolean isWithinRenderDistance(int adjX, int adjY, int adjZ) {
        int x = Math.abs(adjX - this.centerChunkX);
        int z = Math.abs(adjZ - this.centerChunkZ);

        return x <= this.renderDistance && z <= this.renderDistance;
    }

    private boolean isCulled(int sectionId, int from, int to) {
        if (this.state.canCull(sectionId, to)) {
            return true;
        }

        return this.useOcclusionCulling && from != -1 && !this.state.isVisibleThrough(sectionId, from, to);
    }

    private void initSearch(Camera camera, Frustum frustum, int frame, boolean spectator) {
        this.currentFrame = frame;
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

        int rootRenderId = this.state.getSectionId(chunkX, chunkY, chunkZ);
        var rootRender = this.state.getSection(rootRenderId);

        if (rootRender != null) {
            if (spectator && this.world.getBlockState(origin).isOpaqueFullCube(this.world, origin)) {
                this.useOcclusionCulling = false;
            }

            this.iterationQueue.add(rootRenderId, -1);
            this.addChunkToRenderLists(rootRenderId);
        } else {
            chunkY = MathHelper.clamp(origin.getY() >> 4, this.world.getBottomSectionCoord(), this.world.getTopSectionCoord() - 1);

            IntArrayList sorted = new IntArrayList();

            for (int x2 = -this.renderDistance; x2 <= this.renderDistance; ++x2) {
                for (int z2 = -this.renderDistance; z2 <= this.renderDistance; ++z2) {
                    var sectionId = this.state.getSectionId(chunkX + x2, chunkY, chunkZ + z2);

                    if (!this.state.isLoaded(sectionId)) {
                        continue;
                    }

                    if (this.isCulledByView(chunkX + x2, chunkY, chunkZ + z2)) {
                        continue;
                    }

                    sorted.add(sectionId);
                }
            }
//            TODO: FIX
//            sorted.sort(Comparator.comparingDouble(node -> node.getSquaredDistance(origin)));

            IntIterator it = sorted.iterator();
            while (it.hasNext()) {
                var sectionId = it.nextInt();
                this.iterationQueue.add(sectionId, -1);
                this.addChunkToRenderLists(sectionId);
            }
        }
    }

    private boolean raycast(float originX, float originY, float originZ,
                            float directionX, float directionY, float directionZ,
                            float maxDistance) {
        int currVoxelX = MathHelper.floor(originX);
        int currVoxelY = MathHelper.floor(originY);
        int currVoxelZ = MathHelper.floor(originZ);

        final int stepX = sign(directionX);
        final int stepY = sign(directionY);
        final int stepZ = sign(directionZ);

        final float tDeltaX = (stepX == 0) ? Float.MAX_VALUE : (stepX / directionX);
        final float tDeltaY = (stepY == 0) ? Float.MAX_VALUE : (stepY / directionY);
        final float tDeltaZ = (stepZ == 0) ? Float.MAX_VALUE : (stepZ / directionZ);

        float tMaxX = tDeltaX * (stepX > 0 ? frac1(originX) : frac0(originX));
        float tMaxY = tDeltaY * (stepY > 0 ? frac1(originY) : frac0(originY));
        float tMaxZ = tDeltaZ * (stepZ > 0 ? frac1(originZ) : frac0(originZ));

        maxDistance /= (float) Math.sqrt(directionX * directionX + directionY * directionY + directionZ * directionZ);

        int invalid = 0;
        int valid = 0;

        while ((valid < 3 || invalid > 1)) {
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    if (tMaxX > maxDistance) break;
                    currVoxelX += stepX;
                    tMaxX += tDeltaX;
                } else {
                    if (tMaxZ > maxDistance) break;
                    currVoxelZ += stepZ;
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    if (tMaxY > maxDistance) break;
                    currVoxelY += stepY;
                    tMaxY += tDeltaY;
                } else {
                    if (tMaxZ > maxDistance) break;
                    currVoxelZ += stepZ;
                    tMaxZ += tDeltaZ;
                }
            }

            if (currVoxelY < this.bottomSectionCoord || currVoxelY > this.topSectionCoord) {
                break;
            }

            if (this.state.isVisible(this.state.getSectionId(currVoxelX, currVoxelY, currVoxelZ))) {
                valid++;
            } else {
                invalid++;
            }
        }

        return invalid >= 2;
    }

    private boolean isCulledByRaycast(int sectionX, int sectionY, int sectionZ, int flow) {
        float rX = (sectionX << 4) + 8;
        float rY = (sectionY << 4) + 8;
        float rZ = (sectionZ << 4) + 8;

        Direction dir = DirectionUtil.ALL_DIRECTIONS[flow];
        if (dir.getAxis() == Direction.Axis.X) {
            rX += this.centerChunkX > sectionX ? 16.0f : 0.0f;
        } else {
            rX += this.centerChunkX < sectionX ? 16.0f : 0.0f;
        }

        if (dir.getAxis() == Direction.Axis.Y) {
            rY += this.centerChunkY > sectionY ? 16.0f : 0.0f;
        } else {
            rY += this.centerChunkY < sectionY ? 16.0f : 0.0f;
        }

        if (dir.getAxis() == Direction.Axis.Z) {
            rZ += this.centerChunkZ > sectionZ ? 16.0f : 0.0f;
        } else {
            rZ += this.centerChunkZ < sectionZ ? 16.0f : 0.0f;
        }

        float dX = this.cameraX - rX;
        float dY = this.cameraY - rY;
        float dZ = this.cameraZ - rZ;

        float scalar = 1.0f / (float) Math.sqrt(dX * dX + dY * dY + dZ * dZ);
        dX = dX * scalar;
        dY = dY * scalar;
        dZ = dZ * scalar;

        return this.raycast(rX / 16.0f, rY / 16.0f, rZ / 16.0f, dX, dY, dZ, 60.0f / 16.0f);
    }

    private static float frac0(float number) {
        return number - (float) Math.floor(number);
    }

    private static float frac1(float number) {
        return 1.0f - number + (float) Math.floor(number);
    }

    private static int sign(float number) {
        return number == 0 ? 0 : (number > 0 ? 1 : -1);
    }

    private void bfsEnqueue(int parentId, int sectionId,
                            int sectionX, int sectionY, int sectionZ,
                            int dir, boolean doRayCull) {
        if (this.state.isVisible(sectionId)) {
            return;
        }

        if (this.isCulledByView(sectionX, sectionY, sectionZ)) {
            return;
        }

        if (doRayCull && this.isCulledByRaycast(sectionX, sectionY, sectionZ, dir)) {
            return;
        }

        this.state.setCullingState(sectionId, this.state.getCullingState(parentId), dir);
        this.state.setVisible(sectionId);

        this.iterationQueue.add(sectionId, dir);
        this.addChunkToRenderLists(sectionId);
    }

    private void addChunkToRenderLists(int sectionId) {
        var section = this.state.getSection(sectionId);

        if (section.hasFlag(ChunkDataFlags.HAS_BLOCK_GEOMETRY)) {
            this.chunkRenderList.add(section);
        }

        if (section.hasFlag(ChunkDataFlags.HAS_ANIMATED_SPRITES)) {
            this.tickableChunks.add(sectionId);
        }

        if (section.hasFlag(ChunkDataFlags.HAS_BLOCK_ENTITIES)) {
            this.entityChunks.add(sectionId);
        }
    }

    public Collection<String> getDebugStrings() {
        List<String> list = new ArrayList<>();

        Iterator<RenderRegion.RenderRegionArenas> it = this.regions.getLoadedRegions()
                .stream()
                .map(RenderRegion::getArenas)
                .filter(Objects::nonNull)
                .iterator();

        int count = 0;

        long deviceUsed = 0;
        long deviceAllocated = 0;

        while (it.hasNext()) {
            RenderRegion.RenderRegionArenas arena = it.next();
            deviceUsed += arena.getDeviceUsedMemory();
            deviceAllocated += arena.getDeviceAllocatedMemory();

            count++;
        }

        list.add(String.format("Chunk arena allocator: %s", SodiumClientMod.options().advanced.arenaMemoryAllocator.name()));
        list.add(String.format("Device buffer objects: %d", count));
        list.add(String.format("Device memory: %d/%d MiB", MathUtil.toMib(deviceUsed), MathUtil.toMib(deviceAllocated)));
        list.add(String.format("Staging buffer: %s", this.regions.getStagingBuffer().toString()));
        return list;
    }

    private boolean isCulledByView(int chunkX, int chunkY, int chunkZ) {
        float centerX = (chunkX << 4) + 8;
        float centerY = (chunkY << 4) + 8;
        float centerZ = (chunkZ << 4) + 8;

        float distanceX = this.cameraX - centerX;
        float distanceZ = this.cameraZ - centerZ;

        if (this.useFogCulling && (distanceX * distanceX) + (distanceZ * distanceZ) >= this.fogRenderCutoff) {
            return true;
        }

        return !this.frustum.isBoxVisible(centerX - 8.0f, centerY - 8.0f, centerZ - 8.0f,
                centerX + 8.0f, centerY + 8.0f, centerZ + 8.0f);
    }

    private final LongSet readyColumns = new LongOpenHashSet();
    public void onChunkDataChanged(int x, int z) {
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                int adjX = x + offsetX;
                int adjZ = z + offsetZ;

                if (!this.canSubmitChunkForRebuilds(adjX, adjZ)) {
                    continue;
                }

                if (!this.readyColumns.add(ChunkPos.toLong(adjX, adjZ))) {
                    continue;
                }

                this.scheduleReadyChunk(adjX, adjZ);
            }
        }
    }

    private boolean canSubmitChunkForRebuilds(int x, int z) {
        return this.tracker.hasMergedFlags(x, z, ChunkStatus.FLAG_ALL);
    }

    private void scheduleReadyChunk(int x, int z) {
        Chunk chunk = this.world.getChunk(x, z);

        for (int y = this.world.getBottomSectionCoord(); y < this.world.getTopSectionCoord(); y++) {
            var render = this.state.getSection(this.state.getSectionId(x, y, z));

            if (render == null) {
                throw new IllegalStateException("Tried to mark section as ready, but it wasn't loaded? [x=%s, y=%s, z=%s]".formatted(x, y, z));
            }

            ChunkSection section = chunk.getSectionArray()[this.world.sectionCoordToIndex(y)];

            if (section.isEmpty()) {
                render.setData(ChunkRenderData.EMPTY);
            } else {
                render.markForUpdate(ChunkUpdateType.INITIAL_BUILD);
            }
        }
    }

    public void notifyChunksChanged(LongSet dirtyChunks) {
        var it = dirtyChunks.iterator();
        while (it.hasNext()) {
            var key = it.nextLong();
            this.onChunkDataChanged(ChunkPos.getPackedX(key), ChunkPos.getPackedZ(key));
        }
    }
}
