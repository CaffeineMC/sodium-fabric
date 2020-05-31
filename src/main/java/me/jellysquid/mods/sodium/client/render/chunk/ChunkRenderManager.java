package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.*;
import me.jellysquid.mods.sodium.client.render.FrustumExtended;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderState;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPassManager;
import me.jellysquid.mods.sodium.client.util.RenderList;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListener;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import me.jellysquid.mods.sodium.common.util.collections.FutureDequeDrain;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3d;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.*;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ChunkRenderManager<T extends ChunkRenderState> implements ChunkStatusListener {
    /**
     * The maximum distance a chunk can be from the player's camera in order to be eligible for blocking updates.
     */
    private static final double NEARBY_CHUNK_DISTANCE = Math.pow(48, 2.0);

    private final ChunkBuilder<T> builder;
    private final ChunkRenderBackend<T> backend;

    private final Long2ObjectOpenHashMap<ColumnRender<T>> columns = new Long2ObjectOpenHashMap<>();

    private final ObjectList<ColumnRender<T>> unloadQueue = new ObjectArrayList<>();

    private final ArrayDeque<ChunkRenderContainer<T>> importantDirtyChunks = new ArrayDeque<>();
    private final ArrayDeque<ChunkRenderContainer<T>> dirtyChunks = new ArrayDeque<>();
    private final ObjectList<ChunkRenderContainer<T>> tickableChunks = new ObjectArrayList<>();

    @SuppressWarnings("unchecked")
    private final RenderList<T>[] renderLists = new RenderList[BlockRenderPass.count()];

    private final ObjectList<BlockEntity> visibleBlockEntities = new ObjectArrayList<>();

    private final ObjectSet<BlockRenderPass> renderedLayers = new ObjectOpenHashSet<>();

    private final ObjectArrayFIFOQueue<ChunkRenderContainer<T>> iterationQueue = new ObjectArrayFIFOQueue<>();

    private final SodiumWorldRenderer renderer;
    private final ClientWorld world;

    private final int renderDistance;

    private int lastFrameUpdated;
    private boolean useCulling;
    private boolean dirty;

    private int countRenderedSection;
    private int countVisibleSection;

    public ChunkRenderManager(SodiumWorldRenderer renderer, ChunkRenderBackend<T> backend, BlockRenderPassManager renderPassManager, ClientWorld world, int renderDistance) {
        this.backend = backend;
        this.renderer = renderer;
        this.world = world;
        this.renderDistance = renderDistance;

        for (int i = 0; i < this.renderLists.length; i++) {
            this.renderLists[i] = new RenderList<>();
        }

        this.builder = new ChunkBuilder<>(backend.getVertexFormat(), this.backend);
        this.builder.init(world, renderPassManager);

        this.dirty = true;
    }

    public void updateGraph(Camera camera, Vec3d cameraPos, BlockPos blockPos, int frame, FrustumExtended frustum, boolean spectator) {
        this.init(blockPos, camera, cameraPos, frustum, frame, spectator);

        ObjectArrayFIFOQueue<ChunkRenderContainer<T>> queue = this.iterationQueue;

        while (!queue.isEmpty()) {
            ChunkRenderContainer<T> render = queue.dequeue();

            this.addToLists(render);
            this.addNeighbors(render, frustum, frame);
        }

        this.dirty = false;
    }

    private void addToLists(ChunkRenderContainer<T> render) {
        render.setLastVisibleFrame(this.lastFrameUpdated);

        if (render.needsRebuild() && render.getColumn().hasNeighborChunkData()) {
            if (render.needsImportantRebuild()) {
                this.importantDirtyChunks.add(render);
            } else {
                this.dirtyChunks.add(render);
            }
        }

        if (render.canTick()) {
            this.tickableChunks.add(render);
        }

        if (!render.isEmpty()) {
            this.countVisibleSection++;

            T[] states = render.getRenderStates();

            for (int i = 0; i < states.length; i++) {
                T state = states[i];

                if (state != null) {
                    this.renderLists[i].add(state);
                }
            }

            Collection<BlockEntity> blockEntities = render.getData().getBlockEntities();

            if (!blockEntities.isEmpty()) {
                this.visibleBlockEntities.addAll(blockEntities);
            }
        }
    }

    private void addNeighbors(ChunkRenderContainer<T> render, FrustumExtended frustum, int frame) {
        ColumnRender<T> column = render.getColumn();

        for (Direction dir : DirectionUtil.HORIZONTAL_DIRECTIONS) {
            ColumnRender<T> adjColumn = column.getNeighbor(dir);

            if (adjColumn != null) {
                ChunkRenderContainer<T> adj = adjColumn.getChunk(render.getChunkY());

                if (adj != null && adj.getLastGraphUpdateFrame() != frame) {
                    this.addNeighbor(render, adj, dir, frustum, frame);
                }
            }
        }

        for (Direction dir : DirectionUtil.VERTICAL_DIRECTIONS) {
            ChunkRenderContainer<T> adj = column.getChunk(render.getChunkY() + dir.getOffsetY());

            if (adj != null && adj.getLastGraphUpdateFrame() != frame) {
                this.addNeighbor(render, adj, dir, frustum, frame);
            }
        }
    }

    private void addNeighbor(ChunkRenderContainer<T> render, ChunkRenderContainer<T> adj, Direction dir, FrustumExtended frustum, int frame) {
        if (this.useCulling) {
            if (render.canCull(dir)) {
                return;
            }

            if (render.hasData()) {
                Direction flow = render.getDirection();

                if (flow != null && !render.isVisibleThrough(flow.getOpposite(), dir)) {
                    return;
                }
            }

            if (!adj.isVisible(frustum)) {
                return;
            }
        }

        adj.setDirection(dir);
        adj.setLastGraphUpdateFrame(frame);
        adj.updateCullingState(render.getCullingState(), dir.getOpposite());

        this.iterationQueue.enqueue(adj);
    }

    private void init(BlockPos origin, Camera camera, Vec3d cameraPos, FrustumExtended frustum, int frame, boolean spectator) {
        this.lastFrameUpdated = frame;

        this.resetGraph();

        MinecraftClient client = MinecraftClient.getInstance();
        ObjectArrayFIFOQueue<ChunkRenderContainer<T>> queue = this.iterationQueue;

        boolean cull = client.chunkCullingEnabled;

        ChunkRenderContainer<T> node = this.getRenderForBlock(origin.getX(), origin.getY(), origin.getZ());

        if (node != null) {
            node.resetGraphState();

            // Player is within bounds and inside a node
            Set<Direction> openFaces = this.getOpenChunkFaces(origin);

            if (openFaces.size() == 1) {
                Vector3f vector3f = camera.getHorizontalPlane();
                Direction direction = Direction.getFacing(vector3f.getX(), vector3f.getY(), vector3f.getZ()).getOpposite();

                openFaces.remove(direction);
            }

            if (!openFaces.isEmpty() || spectator) {
                if (spectator && this.world.getBlockState(origin).isFullOpaque(this.world, origin)) {
                    cull = false;
                }

                node.setLastGraphUpdateFrame(frame);
            }

            queue.enqueue(node);
        } else {
            // Player is out-of-bounds
            int y = origin.getY() > 0 ? 248 : 8;

            int x = MathHelper.floor(cameraPos.x / 16.0D) * 16;
            int z = MathHelper.floor(cameraPos.z / 16.0D) * 16;

            List<ChunkRenderContainer<T>> list = new ArrayList<>();

            for (int x2 = -this.renderDistance; x2 <= this.renderDistance; ++x2) {
                for (int z2 = -this.renderDistance; z2 <= this.renderDistance; ++z2) {
                    ChunkRenderContainer<T> chunk = this.getRenderForBlock(x + (x2 << 4) + 8, y, z + (z2 << 4) + 8);

                    if (chunk == null || !chunk.isVisible(frustum)) {
                        continue;
                    }

                    chunk.setLastGraphUpdateFrame(frame);
                    chunk.resetGraphState();

                    list.add(chunk);
                }
            }

            list.sort(Comparator.comparingDouble(o -> o.getSquaredDistance(origin)));

            for (ChunkRenderContainer<T> render : list) {
                queue.enqueue(render);
            }
        }

        this.useCulling = cull;
    }

    public ChunkRenderContainer<T> getRenderForBlock(int x, int y, int z) {
        return this.getRender(x >> 4, y >> 4, z >> 4);
    }

    public ChunkRenderContainer<T> getRender(int x, int y, int z) {
        ColumnRender<T> column = this.columns.get(ChunkPos.toLong(x, z));

        if (column != null) {
            return column.getChunk(y);
        }

        return null;
    }

    private Set<Direction> getOpenChunkFaces(BlockPos pos) {
        WorldChunk chunk = this.world.getChunk(pos.getX() >> 4, pos.getZ() >> 4);

        ChunkSection section = chunk.getSectionArray()[pos.getY() >> 4];

        if (section == null || section.isEmpty()) {
            return EnumSet.allOf(Direction.class);
        }

        ChunkOcclusionDataBuilder occlusionBuilder = new ChunkOcclusionDataBuilder();

        BlockState airState = Blocks.AIR.getDefaultState();
        BlockPos.Mutable mpos = new BlockPos.Mutable();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 16; y++) {
                    BlockState state = section.getBlockState(x, y, z);

                    if (state == airState) {
                        continue;
                    }

                    mpos.set(x + pos.getX(), y + pos.getY(), z + pos.getZ());

                    if (state.isFullOpaque(this.world, mpos)) {
                        occlusionBuilder.markClosed(mpos);
                    }
                }
            }
        }

        return occlusionBuilder.getOpenFaces(pos);
    }

    private void resetGraph() {
        this.dirtyChunks.clear();
        this.tickableChunks.clear();
        this.importantDirtyChunks.clear();

        this.visibleBlockEntities.clear();

        for (RenderList<T> renderList : this.renderLists) {
            if (renderList != null) {
                renderList.clear();
            }
        }

        this.countRenderedSection = 0;
        this.countVisibleSection = 0;
    }

    public RenderList<T> getRenderList(BlockRenderPass pass) {
        return this.renderLists[pass.ordinal()];
    }

    public void cleanup() {
        if (!this.unloadQueue.isEmpty()) {
            for (ColumnRender<T> column : this.unloadQueue) {
                if (!column.isChunkPresent()) {
                    this.unloadColumn(column);
                }
            }

            this.unloadQueue.clear();
            this.dirty = true;
        }
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
        this.enqueueChunkUnload(x, z);
    }

    private void loadChunk(int x, int z) {
        ColumnRender<T> column = this.columns.get(ChunkPos.toLong(x, z));

        if (column == null) {
            this.columns.put(ChunkPos.toLong(x, z), column = this.createColumn(x, z));
        }

        column.setChunkPresent(true);
    }

    private void enqueueChunkUnload(int x, int z) {
        ColumnRender<T> column = this.getRenderColumn(x, z);

        if (column != null) {
            column.setChunkPresent(false);

            this.unloadQueue.add(column);
        }
    }

    private ColumnRender<T> createColumn(int x, int z) {
        ColumnRender<T> column = new ColumnRender<>(this.renderer, this.world, x, z, this::createChunkRender);

        for (Direction dir : DirectionUtil.HORIZONTAL_DIRECTIONS) {
            ColumnRender<T> adj = this.getRenderColumn(x + dir.getOffsetX(), z + dir.getOffsetZ());
            column.setNeighbor(dir, adj);

            if (adj != null) {
                adj.setNeighbor(dir.getOpposite(), column);
            }
        }

        return column;
    }

    private ChunkRenderContainer<T> createChunkRender(ColumnRender<T> column, int x, int y, int z) {
        return new ChunkRenderContainer<>(this.backend, column, x, y, z);
    }

    private void unloadColumn(ColumnRender<T> column) {
        column.delete();

        for (Direction dir : DirectionUtil.HORIZONTAL_DIRECTIONS) {
            ColumnRender<T> adj = column.getNeighbor(dir);

            if (adj != null) {
                adj.setNeighbor(dir.getOpposite(), null);
            }
        }

        this.columns.remove(column.getChunkPosLong());
    }

    private ColumnRender<T> getRenderColumn(int x, int z) {
        return this.columns.get(ChunkPos.toLong(x, z));
    }

    public void renderLayer(MatrixStack matrixStack, BlockRenderPass pass, double x, double y, double z) {
        if (this.renderedLayers.isEmpty()) {
            this.tickRenders();
        }

        if (!this.renderedLayers.add(pass)) {
            return;
        }

        RenderList<T> renderList = this.getRenderList(pass);

        if (renderList == null) {
            return;
        }

        this.backend.render(renderList.iterator(pass.isTranslucent()), matrixStack, x, y, z);
    }

    private void tickRenders() {
        for (ChunkRenderContainer<T> render : this.tickableChunks) {
            render.tick();
        }
    }

    public void onFrameChanged() {
        this.renderedLayers.clear();
    }

    public boolean isChunkVisible(int x, int y, int z) {
        ChunkRenderContainer<T> render = this.getRender(x, y, z);

        return render != null && render.getLastVisibleFrame() == this.lastFrameUpdated;
    }

    public void updateChunks() {
        Deque<CompletableFuture<ChunkBuildResult<T>>> futures = new ArrayDeque<>();

        int budget = this.builder.getSchedulingBudget();
        int submitted = 0;

        while (!this.importantDirtyChunks.isEmpty()) {
            ChunkRenderContainer<T> render = this.importantDirtyChunks.remove();

            // Do not allow distant chunks to block rendering
            if (this.isChunkNearby(render)) {
                futures.add(this.builder.scheduleRebuildTaskAsync(render));
            } else {
                this.builder.deferRebuild(render);
            }

            this.dirty = true;
            submitted++;
        }

        while (submitted < budget && !this.dirtyChunks.isEmpty()) {
            ChunkRenderContainer<T> render = this.dirtyChunks.remove();

            this.builder.deferRebuild(render);
            submitted++;
        }

        this.dirty |= submitted > 0;

        // Try to complete some other work on the main thread while we wait for rebuilds to complete
        this.dirty |= this.builder.performPendingUploads();
        this.cleanup();

        this.backend.upload(new FutureDequeDrain<>(futures));
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
        for (ColumnRender<T> column : this.columns.values()) {
            column.delete();
        }

        this.columns.clear();
        this.unloadQueue.clear();

        this.resetGraph();

        this.builder.stopWorkers();
    }

    public int getRenderedSectionCount() {
        return this.countRenderedSection;
    }

    public int getVisibleSectionCount() {
        return this.countVisibleSection;
    }

    public int getTotalSections() {
        return this.columns.size() * 16;
    }

    public void scheduleRebuild(int x, int y, int z, boolean important) {
        ChunkRenderContainer<T> render = this.getRender(x, y, z);

        if (render != null) {
            render.scheduleRebuild(important);

            this.dirty = true;
        }
    }

    private boolean isChunkNearby(ChunkRenderContainer<T> render) {
        Vector3d camera = this.builder.getCameraPosition();
        return render.getSquaredDistance(camera.x, camera.y, camera.z) <= NEARBY_CHUNK_DISTANCE;
    }
}
