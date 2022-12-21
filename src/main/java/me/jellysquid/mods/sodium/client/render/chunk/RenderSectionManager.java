package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
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
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class RenderSectionManager {
    private final ChunkBuilder builder;

    private final RenderRegionManager regions;
    private final EnumMap<ChunkUpdateType, PriorityQueue<RenderSection>> rebuildQueues = new EnumMap<>(ChunkUpdateType.class);

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

    private boolean useOcclusionCulling;

    private Frustum frustum;

    private int currentFrame = 0;
    private boolean alwaysDeferChunkUpdates;

    private static class State {
        private static final long DEFAULT_VISIBILITY_DATA = calculateVisibilityData(ChunkRenderData.EMPTY.getOcclusionData());

        private final int shiftXZ, shiftY;
        private final int maskXZ, maskY;

        public final RenderSection[] sections;
        public final long[] visibilityData;

        public final long[] queuedChunks;
        public final byte[] cullingState;

        public State(World world, int renderDistance) {
            int sizeXZ = MathHelper.smallestEncompassingPowerOfTwo((renderDistance * 2) + 1);
            int sizeY = MathHelper.smallestEncompassingPowerOfTwo(world.getTopSectionCoord() - world.getBottomSectionCoord());

            this.maskXZ = sizeXZ - 1;
            this.maskY = sizeY - 1;

            this.shiftXZ = Integer.numberOfTrailingZeros(sizeXZ);
            this.shiftY = Integer.numberOfTrailingZeros(sizeY);

            int arraySize = sizeXZ * sizeY * sizeXZ;

            this.sections = new RenderSection[arraySize];
            this.visibilityData = new long[arraySize];

            this.queuedChunks = BitArray.create(arraySize);
            this.cullingState = new byte[arraySize];

            Arrays.fill(this.visibilityData, DEFAULT_VISIBILITY_DATA);
        }

        public void reset() {
            BitArray.clear(this.queuedChunks);
            Arrays.fill(this.cullingState, (byte) 0);
        }

        public int getIndex(int x, int y, int z) {
            return (x & this.maskXZ) << this.shiftXZ + this.shiftY | (y & this.maskY) | (z & this.maskXZ) << this.shiftY;
        }
    }

    private static long calculateVisibilityData(ChunkOcclusionData occlusionData) {
        long visibilityData = 0;

        for (int fromId = 0; fromId < DirectionUtil.COUNT; fromId++) {
            for (int toId = 0; toId < DirectionUtil.COUNT; toId++) {
                var from = DirectionUtil.getEnum(fromId);
                var to = DirectionUtil.getEnum(toId);

                if (occlusionData == null || occlusionData.isVisibleThrough(from, to)) {
                    visibilityData |= (1L << ((fromId * DirectionUtil.COUNT) + toId));
                }
            }
        }

        return visibilityData;
    }

    private static byte mergeCullingState(byte parent, int dir) {
        return (byte) (parent | (1 << dir));
    }

    private static boolean canCull(byte state, int dir) {
        return (state & (1 << dir)) != 0;
    }

    private static boolean canCull(byte state) {
        return state != 0b111111;
    }

    public static boolean isVisibleThrough(long data, int from, int to) {
        return (data & 1L << from * 6 + to) != 0L;
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

        for (ChunkUpdateType type : ChunkUpdateType.values()) {
            this.rebuildQueues.put(type, new ObjectArrayFIFOQueue<>());
        }

        this.bottomSectionCoord = this.world.getBottomSectionCoord();
        this.topSectionCoord = this.world.getTopSectionCoord();

        this.state = new State(this.world, renderDistance);
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

        this.alwaysDeferChunkUpdates = options.performance.alwaysDeferChunkUpdates;
        this.state.reset();
    }

    private void iterateChunks(Camera camera, Frustum frustum, int frame, boolean spectator) {
        this.initSearch(camera, frustum, frame, spectator);

        ChunkGraphIterationQueue queue = this.iterationQueue;

        Vector3f cameraPos = new Vector3f((float) camera.getPos().x, (float) camera.getPos().y, (float) camera.getPos().z);
        Vector3f cameraSectionOrigin = new Vector3f(MathHelper.floor(cameraPos.x / 16.0) * 16, MathHelper.floor(cameraPos.y / 16.0) * 16, MathHelper.floor(cameraPos.z / 16.0) * 16);

        boolean isRayCullEnabled = SodiumClientMod.options().performance.useRasterOcclusionCulling;

        for (int i = 0; i < queue.size(); i++) {
            var fromId = queue.getSection(i);
            var fromDirection = queue.getDirection(i);

            var from = this.state.sections[fromId];

            this.addSectionToLists(fromId, from);

            if (!canCull(this.state.cullingState[fromId])) {
                continue;
            }

            boolean useOcclusionCulling = this.useOcclusionCulling && fromDirection != -1;
            boolean useRayCulling = useOcclusionCulling && isRayCullEnabled &&
                    (Math.abs(from.getOriginX() - cameraSectionOrigin.x) > 60 ||
                            Math.abs(from.getOriginY() - cameraSectionOrigin.y) > 60 ||
                            Math.abs(from.getOriginZ() - cameraSectionOrigin.z) > 60);

            for (int toDirection = 0; toDirection < DirectionUtil.COUNT; toDirection++) {
                if (useOcclusionCulling && canCull(this.state.cullingState[fromId], toDirection)) {
                    continue;
                }

                if (useRayCulling && !isVisibleThrough(this.state.visibilityData[fromId], fromDirection, toDirection)) {
                    continue;
                }

                var offset = DirectionUtil.getOffset(toDirection);

                int adjX = from.getChunkX() + offset.getX();
                int adjY = from.getChunkY() + offset.getY();
                int adjZ = from.getChunkZ() + offset.getZ();

                var adjId = this.state.getIndex(adjX, adjY, adjZ);

                if (this.state.sections[adjId] != null) {
                    if (BitArray.get(this.state.queuedChunks, adjId)) {
                        continue;
                    }

                    this.bfsEnqueue(fromId, adjId, adjX, adjY, adjZ,
                            DirectionUtil.getOpposite(toDirection), useRayCulling);
                }
            }
        }
    }

    private void addSectionToLists(int sectionId, RenderSection section) {
        if (section.getPendingUpdate() != null) {
            var queue = this.rebuildQueues.get(section.getPendingUpdate());

            if (queue.size() < 32) {
                queue.enqueue(section);
            }
        }

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
                .mapToObj(id -> this.state.sections[id])
                .flatMap(section -> section.getData()
                        .getBlockEntities()
                        .stream())
                .iterator();
    }

    public void renderLayer(ChunkRenderMatrices matrices, BlockRenderPass pass, double x, double y, double z) {
        RenderDevice device = RenderDevice.INSTANCE;
        CommandList commandList = device.createCommandList();

        this.chunkRenderer.render(matrices, commandList, this.chunkRenderList, pass, new ChunkCameraContext(x, y, z));

        commandList.flush();
    }

    public void tickVisibleRenders() {
        var iterator = this.tickableChunks.iterator();
        var sections = this.state.sections;

        while (iterator.hasNext()) {
            var section = sections[iterator.nextInt()];
            section.tick();
        }
    }

    public boolean isSectionVisible(int x, int y, int z) {
        return BitArray.get(this.state.queuedChunks, this.state.getIndex(x, y, z));
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
        RenderSection section = this.state.sections[this.state.getIndex(x, y, z)];

        if (section != null && section.isBuilt()) {
            if (!this.alwaysDeferChunkUpdates && important) {
                section.markForUpdate(ChunkUpdateType.IMPORTANT_REBUILD);
            } else {
                section.markForUpdate(ChunkUpdateType.REBUILD);
            }

            this.needsUpdate = true;
        }
    }

    public void onChunkRenderUpdates(int x, int y, int z, ChunkRenderData data) {
        ChunkOcclusionData occlusionData = data.getOcclusionData();
        this.state.visibilityData[this.state.getIndex(x, y, z)] = calculateVisibilityData(occlusionData);
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

        int rootRenderId = this.state.getIndex(chunkX, chunkY, chunkZ);
        var rootRender = this.state.sections[rootRenderId];

        if (rootRender != null) {
            if (spectator && this.world.getBlockState(origin).isOpaqueFullCube(this.world, origin)) {
                this.useOcclusionCulling = false;
            }

            this.addSectionToQueue(rootRenderId, -1);
        } else {
            chunkY = MathHelper.clamp(origin.getY() >> 4, this.world.getBottomSectionCoord(), this.world.getTopSectionCoord() - 1);

            IntArrayList sorted = new IntArrayList();

            for (int x2 = -this.renderDistance; x2 <= this.renderDistance; ++x2) {
                for (int z2 = -this.renderDistance; z2 <= this.renderDistance; ++z2) {
                    var sectionId = this.state.getIndex(chunkX + x2, chunkY, chunkZ + z2);

                    if (this.state.sections[sectionId] == null || this.isCulledByFrustum(chunkX + x2, chunkY, chunkZ + z2)) {
                        continue;
                    }

                    sorted.add(sectionId);
                }
            }
//            TODO: FIX
//            sorted.sort(Comparator.comparingDouble(node -> node.getSquaredDistance(origin)));

            IntIterator it = sorted.iterator();
            while (it.hasNext()) {
                this.addSectionToQueue(it.nextInt(), -1);
            }
        }
    }

    private boolean raycast(float originX, float originY, float originZ,
                            float directionX, float directionY, float directionZ,
                            float maxDistance) {
        float d = 0.0f;

        int invalid = 0;
        int valid = 0;

        while ((valid < 4 && invalid < 2) && d < maxDistance) {
            d += 1.0f;

            int x = MathHelper.floor(originX + (directionX * d));
            int y = MathHelper.floor(originY + (directionY * d));
            int z = MathHelper.floor(originZ + (directionZ * d));

            if (y < this.bottomSectionCoord || y > this.topSectionCoord) {
                break;
            }

            if (BitArray.get(this.state.queuedChunks, this.state.getIndex(x, y, z))) {
                valid++;
            } else if (this.isCulledByFrustum(x, y, z)) {
                return false;
            } else {
                invalid++;
            }
        }

        return invalid > 1;
    }

    private boolean isCulledByRaycast(int sectionX, int sectionY, int sectionZ, int dir) {
        final int cameraOriginX = (this.centerChunkX << 4) + 8;
        final int cameraOriginY = (this.centerChunkY << 4) + 8;
        final int cameraOriginZ = (this.centerChunkZ << 4) + 8;

        final int chunkOriginX = sectionX << 4;
        final int chunkOriginY = sectionY << 4;
        final int chunkOriginZ = sectionZ << 4;

        var axis = DirectionUtil.getAxis(dir);

        int xOffset;
        int yOffset;
        int zOffset;

        if (axis == Direction.Axis.X) {
            xOffset = cameraOriginX > chunkOriginX ? 16 : 0;
        } else {
            xOffset = cameraOriginX < chunkOriginX ? 16 : 0;
        }

        if (axis == Direction.Axis.Y) {
            yOffset = cameraOriginY > chunkOriginY ? 16 : 0;
        } else {
            yOffset = cameraOriginY < chunkOriginY ? 16 : 0;
        }

        if (axis == Direction.Axis.Z) {
            zOffset = cameraOriginZ > chunkOriginZ ? 16 : 0;
        } else {
            zOffset = cameraOriginZ < chunkOriginZ ? 16 : 0;
        }


        float rX = chunkOriginX + xOffset;
        float rY = chunkOriginY + yOffset;
        float rZ = chunkOriginZ + zOffset;

        float dX = this.cameraX - rX;
        float dY = this.cameraY - rY;
        float dZ = this.cameraZ - rZ;

        float scalar = 1.0f / (float) Math.sqrt(dX * dX + dY * dY + dZ * dZ);
        dX = dX * scalar;
        dY = dY * scalar;
        dZ = dZ * scalar;

        return this.raycast(rX / 16.0f, rY / 16.0f, rZ / 16.0f, dX, dY, dZ, 5.0f);
    }

    private void bfsEnqueue(int fromId, int toId,
                            int toX, int toY, int toZ,
                            int dir, boolean doRayCull) {
        if (this.isCulledByFrustum(toX, toY, toZ)) {
            return;
        }

        if (doRayCull && this.isCulledByRaycast(toX, toY, toZ, dir)) {
            return;
        }

        this.state.cullingState[toId] = mergeCullingState(this.state.cullingState[fromId], dir);

        this.addSectionToQueue(toId, dir);
    }

    private void addSectionToQueue(int sectionId, int dir) {
        BitArray.set(this.state.queuedChunks, sectionId);
        this.iterationQueue.add(sectionId, dir);
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

    private boolean isCulledByFrustum(int chunkX, int chunkY, int chunkZ) {
        float centerX = (chunkX << 4) + 8;
        float centerY = (chunkY << 4) + 8;
        float centerZ = (chunkZ << 4) + 8;

        return !this.frustum.isBoxVisible(centerX - 8.0f, centerY - 8.0f, centerZ - 8.0f,
                centerX + 8.0f, centerY + 8.0f, centerZ + 8.0f);
    }

    public void loadChunk(int x, int z) {
        for (int y = this.world.getBottomSectionCoord(); y < this.world.getTopSectionCoord(); y++) {
            this.loadSection(x, y, z);
        }

        this.needsUpdate = true;
    }

    private void loadSection(int x, int y, int z) {
        var id = this.state.getIndex(x, y, z);

        if (this.state.sections[id] != null) {
            throw new IllegalStateException("Section is already loaded [x=%s, y=%s, z=%s]".formatted(x, y, z));
        }

        RenderRegion region = this.regions.createRegionForChunk(x, y, z);

        RenderSection render = new RenderSection(this.worldRenderer, x, y, z, region);
        region.addChunk(render);

        this.state.sections[id] = render;
        this.state.visibilityData[id] = State.DEFAULT_VISIBILITY_DATA;

        Chunk chunk = this.world.getChunk(x, z);
        ChunkSection section = chunk.getSectionArray()[this.world.sectionCoordToIndex(y)];

        if (section.isEmpty()) {
            render.setData(ChunkRenderData.EMPTY);
        } else {
            render.markForUpdate(ChunkUpdateType.INITIAL_BUILD);
        }
    }


    public void unloadChunk(int x, int z) {
        for (int y = this.world.getBottomSectionCoord(); y < this.world.getTopSectionCoord(); y++) {
            this.unloadSection(x, y, z);
        }

        this.needsUpdate = true;
    }

    private void unloadSection(int x, int y, int z) {
        var id = this.state.getIndex(x, y, z);
        var section = this.state.sections[id];

        if (section == null) {
            throw new IllegalStateException("Section is not loaded " + ChunkSectionPos.from(x, y, z));
        }

        this.state.sections[id] = null;

        section.delete();

        RenderRegion region = section.getRegion();
        region.removeChunk(section);
    }
}
