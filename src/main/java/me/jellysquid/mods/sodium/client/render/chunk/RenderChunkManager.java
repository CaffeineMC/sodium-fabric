package me.jellysquid.mods.sodium.client.render.chunk;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
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
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
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

    private final IdTable<RenderChunk> renders = new IdTable<>(16384);

    private final ObjectArrayFIFOQueue<RenderChunk> importantRebuildQueue = new ObjectArrayFIFOQueue<>();
    private final ObjectArrayFIFOQueue<RenderChunk> rebuildQueue = new ObjectArrayFIFOQueue<>();

    private final Long2ReferenceMap<RenderChunkStatus> statusProcessingQueue = new Long2ReferenceLinkedOpenHashMap<>();

    private final Deque<ChunkBuildResult> uploadQueue = new ConcurrentLinkedDeque<>();

    private final ObjectList<RenderChunk> visibleChunks = new ObjectArrayList<>();
    private final ObjectList<RenderChunk> tickableChunks = new ObjectArrayList<>();

    private final Reference2ObjectMap<RenderRegion, List<RenderChunk>> sortedVisibleChunks = new Reference2ObjectLinkedOpenHashMap<>();

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

        this.regions = new RenderRegionManager(RenderDevice.INSTANCE, this.chunkRenderer);
        this.sectionCache = new ClonedChunkSectionCache(this.world);

        LongIterator it = ((ClientChunkManagerExtended) world.getChunkManager()).getLoadedChunks().iterator();

        while (it.hasNext()) {
            this.statusProcessingQueue.put(it.nextLong(), RenderChunkStatus.LOAD);
        }
    }

    public void update(Camera camera, FrustumExtended frustum, int frame, boolean spectator) {
        this.resetLists();
        this.processStatusChanges();

        this.setup(camera);
        this.iterateChunks(camera, frustum, frame, spectator);
        this.sortChunks();

        this.dirty = false;
    }

    private void sortChunks() {
        this.sortedVisibleChunks.clear();

        for (RenderChunk render : this.visibleChunks) {
            List<RenderChunk> list = this.sortedVisibleChunks.get(render.getRegion());

            if (list == null) {
                this.sortedVisibleChunks.put(render.getRegion(), list = new ObjectArrayList<>(RenderRegion.REGION_SIZE));
            }

            list.add(render);
        }
    }

    private void processStatusChanges() {
        if (this.statusProcessingQueue.isEmpty())  {
            return;
        }

        for (Long2ReferenceMap.Entry<RenderChunkStatus> entry : this.statusProcessingQueue.long2ReferenceEntrySet()) {
            int x = ChunkPos.getPackedX(entry.getLongKey());
            int z = ChunkPos.getPackedZ(entry.getLongKey());

            for (int y = this.world.getBottomSectionCoord(); y < this.world.getTopSectionCoord(); y++) {
                this.processStatusChangeForSection(x, y, z, entry.getValue());
            }
        }

        this.statusProcessingQueue.clear();
        this.dirty = true;
    }

    private void processStatusChangeForSection(int x, int y, int z, RenderChunkStatus status) {
        if (status == RenderChunkStatus.LOAD) {
            this.loadSection(x, y, z);
        } else if (status == RenderChunkStatus.UNLOAD) {
            this.unloadSection(x, y, z);
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
            RenderChunk render = this.renders.get(it.nextInt());

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
            this.addChunkToRenderLists(render);
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

    private void addChunkToRenderLists(RenderChunk render) {
        this.visibleChunks.add(render);

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
        this.visibleChunks.clear();

        this.tickableChunks.clear();
    }

    public Collection<BlockEntity> getVisibleBlockEntities() {
        return this.visibleBlockEntities;
    }

    @Override
    public void onChunkAdded(int x, int z) {
        this.statusProcessingQueue.put(ChunkPos.toLong(x, z), RenderChunkStatus.LOAD);
    }

    @Override
    public void onChunkRemoved(int x, int z) {
        this.statusProcessingQueue.put(ChunkPos.toLong(x, z), RenderChunkStatus.UNLOAD);
    }

    private void loadSection(int x, int y, int z) {
        RenderChunk chunk = this.addChunk(x, y, z);

        this.culler.onSectionLoaded(x, y, z, chunk.getId());
    }

    private void unloadSection(int x, int y, int z) {
        if (this.removeChunk(x, y, z) != null) {
            this.culler.onSectionUnloaded(x, y, z);
        }
    }

    private RenderChunk removeChunk(int x, int y, int z) {
        RenderChunk render = this.regions.removeChunk(x, y, z);

        if (render != null) {
            render.delete();
        }

        return render;
    }

    private RenderChunk addChunk(int x, int y, int z) {
        RenderChunk render = this.regions.createChunk(this.worldRenderer, x, y, z);

        if (ChunkSection.isEmpty(this.world.getChunk(x, z).getSectionArray()[this.world.sectionCoordToIndex(y)])) {
            render.setData(ChunkRenderData.EMPTY);
        } else {
            render.scheduleRebuild(false);
        }

        render.setId(this.renders.add(render));

        return render;
    }

    public void renderLayer(MatrixStack matrixStack, BlockRenderPass pass, double x, double y, double z) {
        RenderDevice device = RenderDevice.INSTANCE;
        CommandList commandList = device.createCommandList();

        this.chunkRenderer.render(matrixStack, commandList, this.sortedVisibleChunks, pass, new ChunkCameraContext(x, y, z));

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
            throw new IllegalStateException("Tried to rebuild a chunk which is disposed");
        }

        render.cancelRebuildTask();

        ChunkRenderContext context = WorldSlice.prepare(this.world, render.getChunkPos(), this.sectionCache);

        if (context == null) {
            return new ChunkRenderEmptyBuildTask(render);
        } else {
            return new ChunkRenderRebuildTask(render, context, render.getRenderOrigin());
        }
    }

    public void markDirty() {
        this.dirty = true;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public boolean isBuildComplete() {
        return this.builder.isBuildQueueEmpty();
    }

    public void destroy() {
        this.resetLists();

        this.regions.delete();
        this.builder.stopWorkers();
    }

    public int getTotalSections() {
        return this.regions.getLoadedChunks();
    }

    public void scheduleRebuild(int x, int y, int z, boolean important) {
        RenderChunk render = this.regions.getChunk(x, y, z);

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

    public int getVisibleChunkCount() {
        return this.visibleChunks.size();
    }

    public void onChunkRenderUpdates(int x, int y, int z, ChunkRenderData data) {
        this.culler.onSectionStateChanged(x, y, z, data.getOcclusionData());
    }

    enum RenderChunkStatus {
        LOAD,
        UNLOAD
    }
}
