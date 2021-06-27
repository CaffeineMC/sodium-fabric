package me.jellysquid.mods.sodium.client.render.chunk;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.*;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.cull.ChunkCuller;
import me.jellysquid.mods.sodium.client.render.chunk.cull.graph.ChunkGraphCuller;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPassManager;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderBuildTask;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderEmptyBuildTask;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderRebuildTask;
import me.jellysquid.mods.sodium.client.util.math.FrustumExtended;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListener;
import me.jellysquid.mods.sodium.client.world.ClientChunkManagerExtended;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSectionCache;
import me.jellysquid.mods.sodium.common.util.IdTable;
import me.jellysquid.mods.sodium.common.util.collections.FutureQueueDrainingIterator;
import me.jellysquid.mods.sodium.common.util.collections.QueueDrainingIterator;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;

public class RenderChunkManager implements ChunkStatusListener {
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

    private final IdTable<RenderChunk> sectionTable = new IdTable<>(16384);
    private final Long2ReferenceMap<RenderChunk> sections = new Long2ReferenceOpenHashMap<>();

    private final ObjectArrayFIFOQueue<RenderChunk> importantRebuildQueue = new ObjectArrayFIFOQueue<>();
    private final ObjectArrayFIFOQueue<RenderChunk> rebuildQueue = new ObjectArrayFIFOQueue<>();

    private final Long2ReferenceMap<RenderChunkStatus> statusProcessingQueue = new Long2ReferenceLinkedOpenHashMap<>();

    private final Deque<ChunkBuildResult> uploadQueue = new ConcurrentLinkedDeque<>();

    private final ChunkRenderList chunkRenderList = new ChunkRenderList();

    private final ObjectList<RenderChunk> tickableChunks = new ObjectArrayList<>();
    private final ObjectList<BlockEntity> visibleBlockEntities = new ObjectArrayList<>();

    private final SodiumWorldRenderer worldRenderer;
    private final ClientWorld world;

    private final ChunkCuller culler;

    private float cameraX, cameraY, cameraZ;
    private boolean dirty;

    private boolean useFogCulling;
    private double fogRenderCutoff;

    public RenderChunkManager(SodiumWorldRenderer worldRenderer, ChunkRenderer chunkRenderer, BlockRenderPassManager renderPassManager, ClientWorld world, int renderDistance) {
        this.chunkRenderer = chunkRenderer;
        this.worldRenderer = worldRenderer;
        this.world = world;

        this.builder = new ChunkBuilder(chunkRenderer.getVertexType());
        this.builder.init(world, renderPassManager);

        this.dirty = true;

        this.culler = new ChunkGraphCuller(world, renderDistance);

        this.regions = new RenderRegionManager(this.chunkRenderer);
        this.sectionCache = new ClonedChunkSectionCache(this.world);

        LongIterator it = ((ClientChunkManagerExtended) world.getChunkManager()).getLoadedChunks().iterator();

        while (it.hasNext()) {
            long pos = it.nextLong();

            this.onChunkAdded(ChunkPos.getPackedX(pos), ChunkPos.getPackedZ(pos));
        }
    }

    public void update(Camera camera, FrustumExtended frustum, int frame, boolean spectator) {
        this.resetLists();
        this.processStatusChanges();

        this.setup(camera);
        this.iterateChunks(camera, frustum, frame, spectator);

        this.dirty = false;
    }

    private void processStatusChanges() {
        if (this.statusProcessingQueue.isEmpty())  {
            return;
        }

        for (Long2ReferenceMap.Entry<RenderChunkStatus> entry : this.statusProcessingQueue.long2ReferenceEntrySet()) {
            int x = ChunkPos.getPackedX(entry.getLongKey());
            int z = ChunkPos.getPackedZ(entry.getLongKey());

            for (int y = this.world.getBottomSectionCoord(); y < this.world.getTopSectionCoord(); y++) {
                this.dirty |= this.processStatusChangeForSection(x, y, z, entry.getValue());
            }
        }

        this.statusProcessingQueue.clear();
        this.dirty = true;
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
        IntList list = this.culler.computeVisible(camera, frustum, frame, spectator);
        IntIterator it = list.iterator();

        while (it.hasNext()) {
            RenderChunk render = this.sectionTable.get(it.nextInt());

            this.addChunk(render);
        }
    }

    private void addChunk(RenderChunk render) {
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

    private boolean canBuildChunk(RenderChunk render) {
        return isChunkLoaded(render, Direction.NORTH) &&
                isChunkLoaded(render, Direction.SOUTH) &&
                isChunkLoaded(render, Direction.EAST) &&
                isChunkLoaded(render, Direction.WEST) &&
                isChunkLoaded(render, Direction.NORTH, Direction.EAST) &&
                isChunkLoaded(render, Direction.SOUTH, Direction.WEST) &&
                isChunkLoaded(render, Direction.EAST, Direction.SOUTH) &&
                isChunkLoaded(render, Direction.WEST, Direction.NORTH);
    }

    private boolean isChunkLoaded(RenderChunk render, Direction dir1, Direction dir2) {
        return this.isChunkLoaded(
                render.getChunkX() + dir1.getOffsetX() + dir2.getOffsetX(),
                render.getChunkZ() + dir1.getOffsetZ() + dir2.getOffsetZ());
    }


    private boolean isChunkLoaded(RenderChunk render, Direction dir) {
        return this.isChunkLoaded(
                render.getChunkX() + dir.getOffsetX(),
                render.getChunkZ() + dir.getOffsetZ());
    }

    private boolean isChunkLoaded(int x, int z) {
        return this.world.getChunk(x, z, ChunkStatus.FULL, false) != null;
    }

    private void addChunkToVisible(RenderChunk render) {
        this.chunkRenderList.add(render);

        if (render.isTickable()) {
            this.tickableChunks.add(render);
        }
    }

    private void addEntitiesToRenderLists(RenderChunk render) {
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

        RenderChunk render = this.createSection(x, y, z, region);
        render.setId(this.sectionTable.add(render));

        Chunk chunk = this.world.getChunk(x, z);
        ChunkSection section = chunk.getSectionArray()[this.world.sectionCoordToIndex(y)];

        if (ChunkSection.isEmpty(section)) {
            render.setData(ChunkRenderData.EMPTY);
        } else {
            render.scheduleRebuild(false);
        }

        this.culler.onSectionLoaded(x, y, z, render.getId());

        return true;
    }

    private RenderChunk createSection(int x, int y, int z, RenderRegion region) {
        RenderChunk chunk = new RenderChunk(this.worldRenderer, x, y, z, region);
        region.addChunk(chunk);

        this.sections.put(ChunkSectionPos.asLong(x, y, z), chunk);

        return chunk;
    }

    private boolean unloadSection(int x, int y, int z) {
        RenderChunk chunk = this.sections.remove(ChunkSectionPos.asLong(x, y, z));

        if (chunk == null) {
            throw new IllegalStateException("Chunk is not loaded: " + ChunkSectionPos.asLong(x, y, z));
        }

        chunk.delete();

        this.culler.onSectionUnloaded(x, y, z);
        this.sectionTable.remove(chunk.getId());

        RenderRegion region = chunk.getRegion();

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
        for (RenderChunk render : this.tickableChunks) {
            render.tick();
        }
    }

    public boolean isChunkVisible(int x, int y, int z) {
        return this.culler.isSectionVisible(x, y, z);
    }

    public void updateChunks() {
        ArrayDeque<CompletableFuture<ChunkBuildResult>> futures = new ArrayDeque<>();

        int budget = this.builder.getSchedulingBudget();
        int submitted = 0;

        while (!this.importantRebuildQueue.isEmpty()) {
            RenderChunk render = this.importantRebuildQueue.dequeue();

            // Do not allow distant chunks to block rendering
            if (!this.isChunkPrioritized(render)) {
                this.deferChunkRebuild(render);
            } else {
                futures.add(this.builder.schedule(this.createRebuildTask(render)));
            }

            this.dirty = true;
            submitted++;
        }

        while (submitted < budget && !this.rebuildQueue.isEmpty()) {
            RenderChunk render = this.rebuildQueue.dequeue();

            this.deferChunkRebuild(render);
            submitted++;
        }

        this.dirty |= submitted > 0;

        // Try to complete some other work on the main thread while we wait for rebuilds to complete
        this.dirty |= this.performPendingUploads();

        if (!futures.isEmpty()) {
            this.regions.upload(RenderDevice.INSTANCE.createCommandList(), new FutureQueueDrainingIterator<>(futures));
        }
    }

    private boolean performPendingUploads() {
        if (this.uploadQueue.isEmpty()) {
            return false;
        }

        this.regions.upload(RenderDevice.INSTANCE.createCommandList(), new QueueDrainingIterator<>(this.uploadQueue));

        return true;
    }

    private void deferChunkRebuild(RenderChunk render) {
        this.builder.schedule(this.createRebuildTask(render))
                .thenAccept(this.uploadQueue::add);
    }

    public ChunkRenderBuildTask createRebuildTask(RenderChunk render) {
        if (render.isDisposed()) {
            throw new IllegalStateException("Tried to rebuild a chunk " + render + " but it has been disposed");
        }

        render.cancelRebuildTask();

        ChunkRenderContext context = WorldSlice.prepare(this.world, render.getChunkPos(), this.sectionCache);

        if (context == null) {
            return new ChunkRenderEmptyBuildTask(render);
        } else {
            return new ChunkRenderRebuildTask(render, context);
        }
    }

    public void markGraphDirty() {
        this.dirty = true;
    }

    public boolean isGraphDirty() {
        return this.dirty;
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
        return this.regions.getLoadedChunks()
                .stream()
                .mapToInt(RenderRegion::getChunkCount)
                .sum();
    }

    public int getVisibleChunkCount() {
        return this.chunkRenderList.getCount();
    }

    public void scheduleRebuild(int x, int y, int z, boolean important) {
        RenderChunk render = this.sections.get(ChunkSectionPos.asLong(x, y, z));

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

            this.dirty = true;
        }

        this.sectionCache.invalidate(x, y, z);
    }

    public boolean isChunkPrioritized(RenderChunk render) {
        return render.getSquaredDistance(this.cameraX, this.cameraY, this.cameraZ) <= NEARBY_CHUNK_DISTANCE;
    }

    public void onChunkRenderUpdates(int x, int y, int z, ChunkRenderData data) {
        this.culler.onSectionStateChanged(x, y, z, data.getOcclusionData());
        this.culler.onSectionStateChanged(x, y, z, data.getOcclusionData());
    }

    enum RenderChunkStatus {
        LOAD,
        UNLOAD
    }
}
