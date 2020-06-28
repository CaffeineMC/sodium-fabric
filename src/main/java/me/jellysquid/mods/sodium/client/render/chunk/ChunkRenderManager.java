package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.util.GlFogHelper;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.FrustumExtended;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderListIterator;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPassManager;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListener;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import me.jellysquid.mods.sodium.common.util.collections.FutureDequeDrain;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.*;
import net.minecraft.world.chunk.ChunkSection;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ChunkRenderManager<T extends ChunkGraphicsState> implements ChunkStatusListener {
    /**
     * The maximum distance a chunk can be from the player's camera in order to be eligible for blocking updates.
     */
    private static final double NEARBY_CHUNK_DISTANCE = Math.pow(48, 2.0);

    private final ChunkBuilder<T> builder;
    private final ChunkRenderBackend<T> backend;

    private final Long2ObjectOpenHashMap<ChunkRenderContainer<T>> renders = new Long2ObjectOpenHashMap<>();

    private final ObjectArrayFIFOQueue<ChunkRenderContainer<T>> iterationQueue = new ObjectArrayFIFOQueue<>();
    private final ObjectArrayFIFOQueue<ChunkRenderContainer<T>> importantRebuildQueue = new ObjectArrayFIFOQueue<>();
    private final ObjectArrayFIFOQueue<ChunkRenderContainer<T>> rebuildQueue = new ObjectArrayFIFOQueue<>();

    @SuppressWarnings("unchecked")
    private final ChunkRenderList<T>[] chunkRenderLists = new ChunkRenderList[BlockRenderPass.COUNT];
    private final ObjectList<ChunkRenderContainer<T>> tickableChunks = new ObjectArrayList<>();

    private final ObjectList<BlockEntity> visibleBlockEntities = new ObjectArrayList<>();

    private final SodiumWorldRenderer renderer;
    private final ClientWorld world;

    private final int renderDistance;

    private int lastFrameUpdated;
    private double fogRenderCutoff;
    private boolean useOcclusionCulling, useFogCulling;
    private boolean dirty;

    private double cameraX, cameraY, cameraZ;
    private boolean useAggressiveCulling;

    private int visibleChunkCount;

    public ChunkRenderManager(SodiumWorldRenderer renderer, ChunkRenderBackend<T> backend, BlockRenderPassManager renderPassManager, ClientWorld world, int renderDistance) {
        this.backend = backend;
        this.renderer = renderer;
        this.world = world;
        this.renderDistance = renderDistance;

        this.builder = new ChunkBuilder<>(backend.getVertexFormat(), this.backend);
        this.builder.init(world, renderPassManager);

        this.dirty = true;

        for (int i = 0; i < this.chunkRenderLists.length; i++) {
            this.chunkRenderLists[i] = new ChunkRenderList<>();
        }
    }

    public void updateGraph(Camera camera, FrustumExtended frustum, int frame, boolean spectator) {
        this.init(camera, frustum, frame, spectator);

        ObjectArrayFIFOQueue<ChunkRenderContainer<T>> queue = this.iterationQueue;

        while (!queue.isEmpty()) {
            ChunkRenderContainer<T> render = queue.dequeue();

            this.setChunkVisible(render);
            this.addChunkNeighbors(render, frustum, frame);
        }

        this.dirty = false;
    }

    private void addChunkNeighbors(ChunkRenderContainer<T> parent, FrustumExtended frustum, int frame) {
        if (this.useFogCulling && parent.getSquaredDistanceXZ(this.cameraX, this.cameraZ) >= this.fogRenderCutoff) {
            return;
        }

        for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
            ChunkRenderContainer<T> adj = parent.getAdjacentRender(dir);

            if (adj == null || adj.getLastVisibleFrame() == frame || parent.canCull(dir)) {
                continue;
            }

            if (this.useOcclusionCulling) {
                Direction flow = parent.getDirection();

                if (flow != null && !parent.isVisibleThrough(flow, dir)) {
                    continue;
                }
            }

            if (adj.isOutsideFrustum(frustum)) {
                continue;
            }

            this.addChunkNeighbor(parent, adj, dir, frame);
        }
    }

    private void addChunkNeighbor(ChunkRenderContainer<T> parent, ChunkRenderContainer<T> adj, Direction dir, int frame) {
        Direction flow = dir.getOpposite();

        adj.setDirection(flow);
        adj.setVisibleFrame(frame);
        adj.setCullingState(parent.getCullingState(), flow);

        this.iterationQueue.enqueue(adj);
    }

    private void setChunkVisible(ChunkRenderContainer<T> render) {
        if (render.needsRebuild() && render.canRebuild()) {
            if (render.needsImportantRebuild()) {
                this.importantRebuildQueue.enqueue(render);
            } else {
                this.rebuildQueue.enqueue(render);
            }
        }

        if (!render.isEmpty()) {
            this.addChunkToRenderLists(render);

            Collection<BlockEntity> blockEntities = render.getData().getBlockEntities();

            if (!blockEntities.isEmpty()) {
                this.visibleBlockEntities.addAll(blockEntities);
            }
        }
    }

    private void addChunkToRenderLists(ChunkRenderContainer<T> render) {
        int visibleFaces = this.computeVisibleFaces(render);

        if (visibleFaces == 0) {
            return;
        }

        boolean added = false;
        T[] states = render.getGraphicsStates();

        for (int i = 0; i < states.length; i++) {
            T state = states[i];

            if (state != null) {
                ChunkRenderList<T> list = this.chunkRenderLists[i];
                list.add(state, visibleFaces);

                added = true;
            }
        }

        if (added) {
            if (render.isTickable()) {
                this.tickableChunks.add(render);
            }

            this.visibleChunkCount++;
        }
    }

    private int computeVisibleFaces(ChunkRenderContainer<T> render) {
        int visibleFaces;

        if (this.useAggressiveCulling) {
            visibleFaces = 1 << ModelQuadFacing.NONE.ordinal();

            ChunkRenderBounds bounds = render.getBounds();

            if (bounds != null) {
                if (this.cameraY > bounds.y1) {
                    visibleFaces |= 1 << ModelQuadFacing.UP.ordinal();
                }

                if (this.cameraY < bounds.y2) {
                    visibleFaces |= 1 << ModelQuadFacing.DOWN.ordinal();
                }

                if (this.cameraX > bounds.x1) {
                    visibleFaces |= 1 << ModelQuadFacing.EAST.ordinal();
                }

                if (this.cameraX < bounds.x2) {
                    visibleFaces |= 1 << ModelQuadFacing.WEST.ordinal();
                }

                if (this.cameraZ > bounds.z1) {
                    visibleFaces |= 1 << ModelQuadFacing.SOUTH.ordinal();
                }

                if (this.cameraZ < bounds.z2) {
                    visibleFaces |= 1 << ModelQuadFacing.NORTH.ordinal();
                }
            }
        } else {
            visibleFaces = 0b1111111;
        }

        return visibleFaces;
    }

    private void init(Camera camera, FrustumExtended frustum, int frame, boolean spectator) {
        this.cameraX = camera.getPos().x;
        this.cameraY = camera.getPos().y;
        this.cameraZ = camera.getPos().z;

        this.lastFrameUpdated = frame;
        this.useOcclusionCulling = MinecraftClient.getInstance().chunkCullingEnabled;
        this.useAggressiveCulling = SodiumClientMod.options().performance.useAggressiveChunkCulling;

        this.resetGraph();

        BlockPos origin = camera.getBlockPos();
        int chunkX = origin.getX() >> 4;
        int chunkY = origin.getY() >> 4;
        int chunkZ = origin.getZ() >> 4;

        ChunkRenderContainer<T> node = this.getRender(chunkX, chunkY, chunkZ);

        if (node != null) {
            node.resetGraphState();
            node.setVisibleFrame(frame);

            if (spectator && this.world.getBlockState(origin).isFullOpaque(this.world, origin)) {
                this.useOcclusionCulling = false;
            }

            this.iterationQueue.enqueue(node);
        } else {
            chunkY = MathHelper.clamp(origin.getY() >> 4, 0, 15);

            List<ChunkRenderContainer<T>> list = new ArrayList<>();

            for (int x2 = -this.renderDistance; x2 <= this.renderDistance; ++x2) {
                for (int z2 = -this.renderDistance; z2 <= this.renderDistance; ++z2) {
                    ChunkRenderContainer<T> chunk = this.getRender(chunkX + x2, chunkY, chunkZ + z2);

                    if (chunk == null || chunk.isOutsideFrustum(frustum)) {
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
            float dist = GlFogHelper.getFogCutoff() + 2.0f;

            if (dist != 0.0f) {
                this.useFogCulling = true;
                this.fogRenderCutoff = dist * dist;
            }
        }
    }

    public ChunkRenderContainer<T> getRender(int x, int y, int z) {
        return this.renders.get(ChunkSectionPos.asLong(x, y, z));
    }

    private void resetGraph() {
        this.rebuildQueue.clear();
        this.importantRebuildQueue.clear();

        this.visibleBlockEntities.clear();

        for (ChunkRenderList<T> list : this.chunkRenderLists) {
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
        for (int y = 0; y < 16; y++) {
            ChunkRenderContainer<T> render = this.renders.computeIfAbsent(ChunkSectionPos.asLong(x, y, z), this::createChunkRender);

            for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
                ChunkRenderContainer<T> adj = this.getRender(x + dir.getOffsetX(), y + dir.getOffsetY(), z + dir.getOffsetZ());

                if (adj != null) {
                    render.setAdjacentRender(dir, adj);
                    adj.setAdjacentRender(dir.getOpposite(), render);
                }
            }
        }

        this.dirty = true;
    }

    private void unloadChunk(int x, int z) {
        for (int y = 0; y < 16; y++) {
            ChunkRenderContainer<T> render = this.renders.remove(ChunkSectionPos.asLong(x, y, z));

            if (render == null) {
                continue;
            }

            for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
                ChunkRenderContainer<T> adj = render.getAdjacentRender(dir);

                if (adj != null) {
                    render.setAdjacentRender(dir, adj);
                    adj.setAdjacentRender(dir.getOpposite(), null);
                }
            }

            render.delete();
        }

        this.dirty = true;
    }

    private ChunkRenderContainer<T> createChunkRender(long pos) {
        int x = ChunkSectionPos.getX(pos);
        int y = ChunkSectionPos.getY(pos);
        int z = ChunkSectionPos.getZ(pos);

        ChunkRenderContainer<T> render = new ChunkRenderContainer<>(this.backend, this.renderer, x, y, z);

        if (ChunkSection.isEmpty(this.world.getChunk(x, z).getSectionArray()[y])) {
            render.setData(ChunkRenderData.EMPTY);
        } else {
            render.scheduleRebuild(false);
        }

        return render;
    }

    public void renderLayer(MatrixStack matrixStack, BlockRenderPass pass, double x, double y, double z) {
        ChunkRenderList<T> chunkRenderList = this.chunkRenderLists[pass.ordinal()];
        ChunkRenderListIterator<T> iterator = chunkRenderList.iterator(pass.isTranslucent());

        this.backend.begin(matrixStack);
        this.backend.render(pass, iterator, matrixStack, new ChunkCameraContext(x, y, z));
        this.backend.end(matrixStack);
    }

    public void tickVisibleRenders() {
        for (ChunkRenderContainer<T> render : this.tickableChunks) {
            render.tick();
        }
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
            if (!this.isChunkNearby(render)) {
                this.builder.deferRebuild(render);
            } else {
                futures.add(this.builder.scheduleRebuildTaskAsync(render));
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
        this.resetGraph();

        for (ChunkRenderContainer<T> render : this.renders.values()) {
            render.delete();
        }

        this.renders.clear();

        this.builder.stopWorkers();
    }

    public int getTotalSections() {
        return this.renders.size();
    }

    public void scheduleRebuild(int x, int y, int z, boolean important) {
        ChunkRenderContainer<T> render = this.getRender(x, y, z);

        if (render != null) {
            render.scheduleRebuild(important);

            this.dirty = true;
        }
    }

    private boolean isChunkNearby(ChunkRenderContainer<T> render) {
        return render.getSquaredDistance(this.cameraX, this.cameraY, this.cameraZ) <= NEARBY_CHUNK_DISTANCE;
    }

    public int getVisibleChunkCount() {
        return this.visibleChunkCount;
    }
}
