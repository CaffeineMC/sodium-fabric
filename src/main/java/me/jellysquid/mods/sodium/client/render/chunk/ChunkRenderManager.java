package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.*;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.util.GlFogHelper;
import me.jellysquid.mods.sodium.client.render.FrustumExtended;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.backends.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPassManager;
import me.jellysquid.mods.sodium.client.util.RenderList;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListener;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import me.jellysquid.mods.sodium.common.util.collections.FutureDequeDrain;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3d;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ChunkRenderManager<T extends ChunkGraphicsState> implements ChunkStatusListener {
    /**
     * The maximum distance a chunk can be from the player's camera in order to be eligible for blocking updates.
     */
    private static final double NEARBY_CHUNK_DISTANCE = Math.pow(48, 2.0);

    private final ChunkBuilder<T> builder;
    private final ChunkRenderBackend<T> backend;

    private final Long2ObjectOpenHashMap<ColumnRender<T>> columns = new Long2ObjectOpenHashMap<>();

    private final ObjectArrayFIFOQueue<ChunkRenderContainer<T>> iterationQueue = new ObjectArrayFIFOQueue<>();
    private final ObjectArrayFIFOQueue<ChunkRenderContainer<T>> importantRebuildQueue = new ObjectArrayFIFOQueue<>();
    private final ObjectArrayFIFOQueue<ChunkRenderContainer<T>> rebuildQueue = new ObjectArrayFIFOQueue<>();
    private final ObjectArrayFIFOQueue<ColumnRender<T>> unloadQueue = new ObjectArrayFIFOQueue<>();

    private final RenderList<ChunkRenderContainer<T>> visibleChunks = new RenderList<>();
    private final ObjectList<BlockEntity> visibleBlockEntities = new ObjectArrayList<>();

    private final ObjectSet<BlockRenderPass> renderedLayers = new ObjectOpenHashSet<>();

    private final SodiumWorldRenderer renderer;
    private final ClientWorld world;

    private final int renderDistance;

    private int lastFrameUpdated;
    private double fogRenderCutoff;
    private boolean useFrustumCulling, useFogCulling;
    private boolean dirty;

    private int countRenderedSection;
    private int countVisibleSection;

    public ChunkRenderManager(SodiumWorldRenderer renderer, ChunkRenderBackend<T> backend, BlockRenderPassManager renderPassManager, ClientWorld world, int renderDistance) {
        this.backend = backend;
        this.renderer = renderer;
        this.world = world;
        this.renderDistance = renderDistance;

        this.builder = new ChunkBuilder<>(backend.getVertexFormat(), this.backend);
        this.builder.init(world, renderPassManager);

        this.dirty = true;
    }

    public void updateGraph(Camera camera, FrustumExtended frustum, int frame, boolean spectator) {
        this.init(camera, frustum, frame, spectator);

        ObjectArrayFIFOQueue<ChunkRenderContainer<T>> queue = this.iterationQueue;

        while (!queue.isEmpty()) {
            ChunkRenderContainer<T> render = queue.dequeue();

            this.addChunkToRenderLists(render);
            this.addChunkNeighbors(render, frustum, frame);
        }

        this.dirty = false;
    }

    private void addChunkNeighbors(ChunkRenderContainer<T> render, FrustumExtended frustum, int frame) {
        if (this.useFogCulling && render.getColumn().getSquaredDistanceXZ(this.builder.getCameraPosition()) >= this.fogRenderCutoff) {
            return;
        }

        for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
            if (render.canCull(dir)) {
                continue;
            }

            if (this.useFrustumCulling) {
                Direction flow = render.getDirection();

                if (flow != null && !render.isVisibleThrough(flow.getOpposite(), dir)) {
                    continue;
                }
            }

            this.addChunkNeighbor(render, dir, frustum, frame);
        }
    }

    private void addChunkNeighbor(ChunkRenderContainer<T> render, Direction dir, FrustumExtended frustum, int frame) {
        ColumnRender<T> column = render.getColumn().getNeighbor(dir);

        if (column != null) {
            ChunkRenderContainer<T> adj = column.getChunk(render.getChunkY() + dir.getOffsetY());

            if (adj == null || adj.getLastVisibleFrame() == frame || !adj.isVisible(frustum)) {
                return;
            }

            adj.setDirection(dir);
            adj.setVisibleFrame(frame);
            adj.setCullingState(render.getCullingState(), dir.getOpposite());

            this.iterationQueue.enqueue(adj);
        }
    }

    private void addChunkToRenderLists(ChunkRenderContainer<T> render) {
        if (render.needsRebuild() && render.canRebuild()) {
            if (render.needsImportantRebuild()) {
                this.importantRebuildQueue.enqueue(render);
            } else {
                this.rebuildQueue.enqueue(render);
            }
        }

        if (!render.isEmpty()) {
            this.countVisibleSection++;

            if (render.getGraphicsState() != null) {
                this.visibleChunks.add(render);
            }

            Collection<BlockEntity> blockEntities = render.getData().getBlockEntities();

            if (!blockEntities.isEmpty()) {
                this.visibleBlockEntities.addAll(blockEntities);
            }
        }
    }

    private void init(Camera camera, FrustumExtended frustum, int frame, boolean spectator) {
        this.lastFrameUpdated = frame;
        this.resetGraph();

        this.useFrustumCulling = MinecraftClient.getInstance().chunkCullingEnabled;

        BlockPos origin = camera.getBlockPos();
        int chunkX = origin.getX() >> 4;
        int chunkY = origin.getY() >> 4;
        int chunkZ = origin.getZ() >> 4;

        ChunkRenderContainer<T> node = this.getRender(chunkX, chunkY, chunkZ);

        if (node != null) {
            node.resetGraphState();
            node.setVisibleFrame(frame);

            if (spectator && this.world.getBlockState(origin).isFullOpaque(this.world, origin)) {
                this.useFrustumCulling = false;
            }

            this.iterationQueue.enqueue(node);
        } else {
            chunkY = MathHelper.clamp(origin.getY() >> 4, 0, 15);

            List<ChunkRenderContainer<T>> list = new ArrayList<>();

            for (int x2 = -this.renderDistance; x2 <= this.renderDistance; ++x2) {
                for (int z2 = -this.renderDistance; z2 <= this.renderDistance; ++z2) {
                    ChunkRenderContainer<T> chunk = this.getRender(chunkX + x2, chunkY, chunkZ + z2);

                    if (chunk == null || !chunk.isVisible(frustum)) {
                        continue;
                    }

                    chunk.setVisibleFrame(frame);
                    chunk.resetGraphState();

                    list.add(chunk);
                }
            }

            list.sort(Comparator.comparingDouble(o -> o.getSquaredDistance(origin)));

            for (ChunkRenderContainer<T> render : list) {
                this.iterationQueue.enqueue(render);
            }
        }

        this.useFogCulling = false;

        if (SodiumClientMod.options().performance.useFogOcclusion) {
            float dist = GlFogHelper.getFogCutoff() - 4.0f;

            if (dist != 0.0f) {
                this.useFogCulling = true;
                this.fogRenderCutoff = dist * dist;
            }
        }
    }

    public ChunkRenderContainer<T> getRender(int x, int y, int z) {
        ColumnRender<T> column = this.columns.get(ChunkPos.toLong(x, z));

        if (column != null) {
            return column.getChunk(y);
        }

        return null;
    }

    private void resetGraph() {
        this.rebuildQueue.clear();
        this.importantRebuildQueue.clear();

        this.visibleBlockEntities.clear();
        this.visibleChunks.clear();

        this.countRenderedSection = 0;
        this.countVisibleSection = 0;
    }

    public void cleanup() {
        while (!this.unloadQueue.isEmpty()) {
            ColumnRender<T> column = this.unloadQueue.dequeue();

            if (!column.isChunkPresent()) {
                this.unloadColumn(column);
            }
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

            this.unloadQueue.enqueue(column);
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
        return new ChunkRenderContainer<>(column, x, y, z);
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
        this.dirty = true;
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

        this.backend.render(pass, this.visibleChunks.iterator(pass.isTranslucent()), matrixStack, x, y, z);
    }

    private void tickRenders() {
        for (ChunkRenderContainer<T> render : this.visibleChunks) {
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

        while (!this.importantRebuildQueue.isEmpty()) {
            ChunkRenderContainer<T> render = this.importantRebuildQueue.dequeue();

            // Do not allow distant chunks to block rendering
            if (this.isChunkNearby(render)) {
                futures.add(this.builder.scheduleRebuildTaskAsync(render));
            } else {
                this.builder.deferRebuild(render);
            }

            this.dirty = true;
            submitted++;
        }

        while (submitted < budget && !this.rebuildQueue.isEmpty()) {
            ChunkRenderContainer<T> render = this.rebuildQueue.dequeue();

            this.builder.deferRebuild(render);
            submitted++;
        }

        this.dirty |= submitted > 0;

        // Try to complete some other work on the main thread while we wait for rebuilds to complete
        this.dirty |= this.builder.performPendingUploads();
        this.cleanup();

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

    public RenderList<ChunkRenderContainer<T>> getVisibleChunks() {
        return this.visibleChunks;
    }
}
