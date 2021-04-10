package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.util.GlFogHelper;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.cull.ChunkCuller;
import me.jellysquid.mods.sodium.client.render.chunk.cull.ChunkFaceFlags;
import me.jellysquid.mods.sodium.client.render.chunk.cull.graph.ChunkGraphCuller;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderBounds;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderListIterator;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPassManager;
import me.jellysquid.mods.sodium.client.util.math.FrustumExtended;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListener;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import me.jellysquid.mods.sodium.common.util.collections.FutureDequeDrain;
import me.jellysquid.mods.sodium.common.util.collections.IntPool;
import me.jellysquid.mods.sodium.common.util.collections.TrackedArray;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;

public class ChunkRenderManager implements ChunkStatusListener {
    /**
     * The maximum distance a chunk can be from the player's camera in order to be eligible for blocking updates.
     */
    private static final float NEARBY_CHUNK_DISTANCE = (float) Math.pow(48, 2.0);

    /**
     * The minimum distance the culling plane can be from the player's camera. This helps to prevent mathematical
     * errors that occur when the fog distance is less than 8 blocks in width, such as when using a blindness potion.
     */
    private static final float FOG_PLANE_MIN_DISTANCE = (float) Math.pow(8.0, 2.0);

    /**
     * The distance past the fog's far plane at which to begin culling. Distance calculations use the center of each
     * chunk from the camera's position, and as such, special care is needed to ensure that the culling plane is pushed
     * back far enough. I'm sure there's a mathematical formula that should be used here in place of the constant,
     * but this value works fine in testing.
     */
    private static final float FOG_PLANE_OFFSET = 12.0f;

    private final ChunkBuilder builder;
    private final ChunkRenderBackend backend;

    private final Long2ObjectOpenHashMap<ChunkRenderColumn> columns = new Long2ObjectOpenHashMap<>();

    private final TrackedArray<ChunkRenderContainer> renders = new TrackedArray<>(ChunkRenderContainer.class, 16384);
    private final IntPool renderIds = new IntPool();

    private final ObjectArrayFIFOQueue<ChunkRenderContainer> importantRebuildQueue = new ObjectArrayFIFOQueue<>();
    private final ObjectArrayFIFOQueue<ChunkRenderContainer> rebuildQueue = new ObjectArrayFIFOQueue<>();

    private final ChunkRenderList[] chunkRenderLists = new ChunkRenderList[BlockRenderPass.COUNT];
    private final ObjectList<ChunkRenderContainer> tickableChunks = new ObjectArrayList<>();

    private final ObjectList<BlockEntity> visibleBlockEntities = new ObjectArrayList<>();

    private final SodiumWorldRenderer renderer;
    private final ClientWorld world;

    private final ChunkCuller culler;
    private final boolean useChunkFaceCulling;

    private float cameraX, cameraY, cameraZ;
    private boolean dirty;

    private int visibleChunkCount;

    private boolean useFogCulling;
    private double fogRenderCutoff;

    public ChunkRenderManager(SodiumWorldRenderer renderer, ChunkRenderBackend backend, BlockRenderPassManager renderPassManager, ClientWorld world, int renderDistance) {
        this.backend = backend;
        this.renderer = renderer;
        this.world = world;

        this.builder = new ChunkBuilder(backend.getVertexType(), this.backend);
        this.builder.init(world, renderPassManager);

        this.dirty = true;

        for (int i = 0; i < this.chunkRenderLists.length; i++) {
            this.chunkRenderLists[i] = new ChunkRenderList();
        }

        this.culler = new ChunkGraphCuller(world, renderDistance);
        this.useChunkFaceCulling = SodiumClientMod.options().advanced.useChunkFaceCulling;
    }

    public void update(Camera camera, FrustumExtended frustum, int frame, boolean spectator) {
        this.reset();

        this.setup(camera);
        this.iterateChunks(camera, frustum, frame, spectator);

        this.dirty = false;
    }

    private void setup(Camera camera) {
        Vec3d cameraPos = camera.getPos();

        this.cameraX = (float) cameraPos.x;
        this.cameraY = (float) cameraPos.y;
        this.cameraZ = (float) cameraPos.z;

        this.useFogCulling = false;

        // Iris: disable fog culling since shaderpacks aren't guaranteed to actually implement fog.
        /*if (SodiumClientMod.options().advanced.useFogOcclusion) {
            float dist = GlFogHelper.getFogCutoff() + FOG_PLANE_OFFSET;

            if (dist != 0.0f) {
                this.useFogCulling = true;
                this.fogRenderCutoff = Math.max(FOG_PLANE_MIN_DISTANCE, dist * dist);
            }
        }*/
    }

    private void iterateChunks(Camera camera, FrustumExtended frustum, int frame, boolean spectator) {
        IntList list = this.culler.computeVisible(camera, frustum, frame, spectator);
        IntIterator it = list.iterator();

        while (it.hasNext()) {
            int sectionId = it.nextInt();
            ChunkRenderContainer render = this.renders.get(sectionId);

            this.addChunk(render);
        }
    }

    private void addChunk(ChunkRenderContainer render) {
        if (render.needsRebuild() && render.canRebuild()) {
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

    private void addChunkToRenderLists(ChunkRenderContainer render) {
        int visibleFaces = render.getFacesWithData();

        if (this.useChunkFaceCulling) {
            ChunkRenderBounds bounds = render.getBounds();

            if (this.cameraX < bounds.x1) {
                visibleFaces &= ~ChunkFaceFlags.EAST;
            }

            if (this.cameraX > bounds.x2) {
                visibleFaces &= ~ChunkFaceFlags.WEST;
            }

            if (this.cameraY < bounds.y1) {
                visibleFaces &= ~ChunkFaceFlags.UP;
            }

            if (this.cameraY > bounds.y2) {
                visibleFaces &= ~ChunkFaceFlags.DOWN;
            }

            if (this.cameraZ < bounds.z1) {
                visibleFaces &= ~ChunkFaceFlags.SOUTH;
            }

            if (this.cameraZ > bounds.z2) {
                visibleFaces &= ~ChunkFaceFlags.NORTH;
            }
        }

        if (visibleFaces == 0) {
            return;
        }

        ChunkGraphicsStateArray states = render.getGraphicsStates();

        for (int i = 0; i < states.getSize(); i++) {
            this.chunkRenderLists[states.getKey(i)]
                    .add(states.getValue(i), visibleFaces);
        }

        if (render.isTickable()) {
            this.tickableChunks.add(render);
        }

        this.visibleChunkCount++;
    }

    private void addEntitiesToRenderLists(ChunkRenderContainer render) {
        Collection<BlockEntity> blockEntities = render.getData().getBlockEntities();

        if (!blockEntities.isEmpty()) {
            this.visibleBlockEntities.addAll(blockEntities);
        }
    }

    public ChunkRenderContainer getRender(int x, int y, int z) {
        ChunkRenderColumn column = this.columns.get(ChunkPos.toLong(x, z));

        if (column == null) {
            return null;
        }

        return column.getRender(y);
    }

    private void reset() {
        this.rebuildQueue.clear();
        this.importantRebuildQueue.clear();

        this.visibleBlockEntities.clear();

        for (ChunkRenderList list : this.chunkRenderLists) {
            list.reset();
        }

        this.tickableChunks.clear();

        this.visibleChunkCount = 0;
    }

    public Collection<BlockEntity> getVisibleBlockEntities() {
        return this.visibleBlockEntities;
    }

    @Override
    public void onChunkAdded(int x, int z) {
        this.builder.onChunkStatusChanged(x, z);
        this.loadChunk(x, z);
    }

    @Override
    public void onChunkRemoved(int x, int z) {
        this.builder.onChunkStatusChanged(x, z);
        this.unloadChunk(x, z);
    }

    private void loadChunk(int x, int z) {
        ChunkRenderColumn column = new ChunkRenderColumn(x, z);
        ChunkRenderColumn prev;

        if ((prev = this.columns.put(ChunkPos.toLong(x, z), column)) != null) {
            this.unloadSections(prev);
        }

        this.connectNeighborColumns(column);
        this.loadSections(column);

        this.dirty = true;
    }

    private void unloadChunk(int x, int z) {
        ChunkRenderColumn column = this.columns.remove(ChunkPos.toLong(x, z));

        if (column == null) {
            return;
        }

        this.disconnectNeighborColumns(column);
        this.unloadSections(column);

        this.dirty = true;
    }

    private void loadSections(ChunkRenderColumn column) {
        int x = column.getX();
        int z = column.getZ();

        for (int y = 0; y < 16; y++) {
            ChunkRenderContainer render = this.createChunkRender(column, x, y, z);
            column.setRender(y, render);

            this.culler.onSectionLoaded(x, y, z, render.getId());
        }
    }

    private void unloadSections(ChunkRenderColumn column) {
        for (int y = 0; y < 16; y++) {
            ChunkRenderContainer render = column.getRender(y);

            if (render != null) {
                render.delete();

                this.renders.remove(render);
                this.renderIds.deallocateId(render.getId());

                this.culler.onSectionUnloaded(render.getId());
            }
        }
    }

    private void connectNeighborColumns(ChunkRenderColumn column) {
        for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
            ChunkRenderColumn adj = this.getAdjacentColumn(column, dir);

            if (adj != null) {
                adj.setAdjacentColumn(dir.getOpposite(), column);
            }

            column.setAdjacentColumn(dir, adj);
        }
    }

    private void disconnectNeighborColumns(ChunkRenderColumn column) {
        for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
            ChunkRenderColumn adj = column.getAdjacentColumn(dir);

            if (adj != null) {
                adj.setAdjacentColumn(dir.getOpposite(), null);
            }

            column.setAdjacentColumn(dir, null);
        }
    }

    private ChunkRenderColumn getAdjacentColumn(ChunkRenderColumn column, Direction dir) {
        return this.getColumn(column.getX() + dir.getOffsetX(), column.getZ() + dir.getOffsetZ());
    }

    private ChunkRenderColumn getColumn(int x, int z) {
        return this.columns.get(ChunkPos.toLong(x, z));
    }

    private ChunkRenderContainer createChunkRender(ChunkRenderColumn column, int x, int y, int z) {
        ChunkRenderContainer render = new ChunkRenderContainer(this.backend, this.renderer, x, y, z, column, this.renderIds.allocateId());
        this.renders.add(render);

        if (ChunkSection.isEmpty(this.world.getChunk(x, z).getSectionArray()[y])) {
            render.setData(ChunkRenderData.EMPTY);
        } else {
            render.scheduleRebuild(false);
        }

        return render;
    }

    public void renderLayer(MatrixStack matrixStack, BlockRenderPass pass, double x, double y, double z) {
        ChunkRenderList chunkRenderList = this.chunkRenderLists[pass.ordinal()];
        ChunkRenderListIterator iterator = chunkRenderList.iterator(pass.isTranslucent());

        this.backend.begin(matrixStack, pass);
        this.backend.render(iterator, new ChunkCameraContext(x, y, z));
        this.backend.end(matrixStack);
    }

    public void tickVisibleRenders() {
        for (ChunkRenderContainer render : this.tickableChunks) {
            render.tick();
        }
    }

    public boolean isChunkVisible(int x, int y, int z) {
        ChunkRenderContainer render = this.getRender(x, y, z);

        if (render == null) {
            return false;
        }

        return this.culler.isSectionVisible(render.getId());
    }

    public void updateChunks() {
        Deque<CompletableFuture<ChunkBuildResult>> futures = new ArrayDeque<>();

        int budget = this.builder.getSchedulingBudget();
        int submitted = 0;

        while (!this.importantRebuildQueue.isEmpty()) {
            ChunkRenderContainer render = this.importantRebuildQueue.dequeue();

            // Do not allow distant chunks to block rendering
            if (!this.isChunkPrioritized(render)) {
                this.builder.deferRebuild(render);
            } else {
                futures.add(this.builder.scheduleRebuildTaskAsync(render));
            }

            this.dirty = true;
            submitted++;
        }

        while (submitted < budget && !this.rebuildQueue.isEmpty()) {
            ChunkRenderContainer render = this.rebuildQueue.dequeue();

            this.builder.deferRebuild(render);
            submitted++;
        }

        this.dirty |= submitted > 0;

        // Try to complete some other work on the main thread while we wait for rebuilds to complete
        this.dirty |= this.builder.performPendingUploads();

        if (!futures.isEmpty()) {
            this.backend.upload(new FutureDequeDrain<>(futures));
        }
    }

    public void markDirty() {
        this.dirty = true;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void restoreChunks(LongCollection chunks) {
        LongIterator it = chunks.iterator();

        while (it.hasNext()) {
            long pos = it.nextLong();

            this.loadChunk(ChunkPos.getPackedX(pos), ChunkPos.getPackedZ(pos));
        }
    }

    public boolean isBuildComplete() {
        return this.builder.isBuildQueueEmpty();
    }

    public void setCameraPosition(double x, double y, double z) {
        this.builder.setCameraPosition(x, y, z);
    }

    public void destroy() {
        this.reset();

        for (ChunkRenderColumn column : this.columns.values()) {
            this.unloadSections(column);
        }

        this.columns.clear();

        this.builder.stopWorkers();
    }

    public int getTotalSections() {
        return this.columns.size() * 16;
    }

    public void scheduleRebuild(int x, int y, int z, boolean important) {
        ChunkRenderContainer render = this.getRender(x, y, z);

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
    }

    public boolean isChunkPrioritized(ChunkRenderContainer render) {
        return render.getSquaredDistance(this.cameraX, this.cameraY, this.cameraZ) <= NEARBY_CHUNK_DISTANCE;
    }

    public int getVisibleChunkCount() {
        return this.visibleChunkCount;
    }

    public void onChunkRenderUpdates(int x, int y, int z, ChunkRenderData data) {
        ChunkRenderContainer render = this.getRender(x, y, z);

        if (render != null) {
            this.culler.onSectionStateChanged(render.getId(), data);
        }
    }
}
