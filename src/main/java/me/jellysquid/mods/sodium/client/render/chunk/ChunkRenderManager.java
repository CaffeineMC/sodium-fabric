package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.*;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderState;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPass;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListener;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;

public class ChunkRenderManager<T extends ChunkRenderState> implements ChunkStatusListener {
    private final ChunkRenderBackend<T> chunkRenderer;
    private final Long2ObjectOpenHashMap<ColumnRender<T>> columns = new Long2ObjectOpenHashMap<>();

    private final ObjectList<ColumnRender<T>> unloadQueue = new ObjectArrayList<>();
    private final ObjectList<ChunkRender<T>> visibleChunks = new ObjectArrayList<>();
    private final ObjectList<ChunkRender<T>> drawableChunks = new ObjectArrayList<>();

    private final ObjectList<BlockEntity> visibleBlockEntities = new ObjectArrayList<>();

    private final ObjectSet<BlockRenderPass> renderedLayers = new ObjectOpenHashSet<>();

    private final ObjectArrayFIFOQueue<ChunkRender<T>> iterationQueue = new ObjectArrayFIFOQueue<>();

    private final ChunkRenderer renderer;
    private final World world;

    private final int renderDistance;

    private int lastFrameUpdated;
    private boolean useCulling;

    public ChunkRenderManager(ChunkRenderBackend<T> chunkRenderer, ChunkRenderer renderer, World world, int renderDistance) {
        this.chunkRenderer = chunkRenderer;
        this.renderer = renderer;
        this.world = world;
        this.renderDistance = renderDistance;
    }

    public void addAllChunks(LongCollection chunks) {
        LongIterator it = chunks.iterator();

        while (it.hasNext()) {
            long pos = it.nextLong();

            this.loadChunk(ChunkPos.getPackedX(pos), ChunkPos.getPackedZ(pos));
        }
    }

    public void calculateVisible(Camera camera, Vec3d cameraPos, BlockPos blockPos, int frame, Frustum frustum, boolean spectator) {
        this.lastFrameUpdated = frame;

        this.visibleChunks.clear();
        this.drawableChunks.clear();
        this.visibleBlockEntities.clear();

        this.init(blockPos, camera, cameraPos, frustum, frame, spectator);

        while (!this.iterationQueue.isEmpty()) {
            ChunkRender<T> render = this.iterationQueue.dequeue();

            this.markVisible(render);
            this.addNeighbors(render, frustum, frame);
        }
    }

    private void markVisible(ChunkRender<T> render) {
        render.setLastVisibleFrame(this.lastFrameUpdated);

        this.visibleChunks.add(render);

        if (!render.isEmpty()) {
            this.drawableChunks.add(render);
        }

        Collection<BlockEntity> blockEntities = render.getData().getBlockEntities();

        if (!blockEntities.isEmpty()) {
            this.visibleBlockEntities.addAll(blockEntities);
        }
    }

    private void addNeighbors(ChunkRender<T> render, Frustum frustum, int frame) {
        for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
            ChunkRender<T> adj = this.getAdjacentRender(render, dir);

            if (adj == null || adj.getRebuildFrame() == frame) {
                continue;
            }

            if (this.useCulling && !this.isVisible(render, adj, dir, frustum, frame)) {
                continue;
            }

            if (!adj.getColumn().hasNeighbors()) {
                continue;
            }

            adj.setDirection(dir);
            adj.setRebuildFrame(frame);
            adj.updateCullingState(render.getCullingState(), dir);

            this.iterationQueue.enqueue(adj);
        }
    }

    private boolean isVisible(ChunkRender<T> render, ChunkRender<T> adjRender, Direction dir, Frustum frustum, int frame) {
        if (render.canCull(dir.getOpposite())) {
            return false;
        }

        if (render.getDirection() != null && !render.isVisibleThrough(render.getDirection().getOpposite(), dir)) {
            return false;
        }

        return adjRender.isVisible(frustum, frame);
    }

    private void init(BlockPos origin, Camera camera, Vec3d cameraPos, Frustum frustum, int frame, boolean spectator) {
        MinecraftClient client = MinecraftClient.getInstance();
        ObjectArrayFIFOQueue<ChunkRender<T>> queue = this.iterationQueue;

        boolean cull = client.chunkCullingEnabled;

        ChunkRender<T> node = this.getRenderForBlock(origin.getX(), origin.getY(), origin.getZ());

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

                node.setRebuildFrame(frame);
            }

            queue.enqueue(node);
        } else {
            // Player is out-of-bounds
            int y = origin.getY() > 0 ? 248 : 8;

            int x = MathHelper.floor(cameraPos.x / 16.0D) * 16;
            int z = MathHelper.floor(cameraPos.z / 16.0D) * 16;

            List<ChunkRender<T>> list = new ArrayList<>();

            for (int x2 = -this.renderDistance; x2 <= this.renderDistance; ++x2) {
                for (int z2 = -this.renderDistance; z2 <= this.renderDistance; ++z2) {
                    ChunkRender<T> chunk = this.getRenderForBlock(x + (x2 << 4) + 8, y, z + (z2 << 4) + 8);

                    if (chunk == null || !frustum.isVisible(chunk.getBoundingBox())) {
                        continue;
                    }

                    chunk.setRebuildFrame(frame);
                    chunk.resetGraphState();

                    list.add(chunk);
                }
            }

            list.sort(Comparator.comparingDouble(o -> o.getSquaredDistance(origin)));

            for (ChunkRender<T> n : list) {
                queue.enqueue(n);
            }
        }

        this.useCulling = cull;
    }

    public ChunkRender<T> getRenderForBlock(int x, int y, int z) {
        return this.getRender(x >> 4, y >> 4, z >> 4);
    }

    public ChunkRender<T> getRender(int x, int y, int z) {
        ColumnRender<T> column = this.columns.get(ChunkPos.toLong(x, z));

        if (column != null) {
            return column.getChunk(y);
        }

        return null;
    }

    private ChunkRender<T> getAdjacentRender(ChunkRender<T> render, Direction direction) {
        ColumnRender<T> column = render.getColumn().getNeighbor(direction);

        if (column != null) {
            return column.getChunk(render.getChunkY() + direction.getOffsetY());
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

        BlockPos.Mutable mpos = new BlockPos.Mutable();

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    BlockState state = section.getBlockState(x, y, z);
                    mpos.set(x + pos.getX(), y + pos.getY(), z + pos.getZ());

                    if (state.isFullOpaque(this.world, mpos)) {
                        occlusionBuilder.markClosed(mpos);
                    }
                }
            }
        }

        return occlusionBuilder.getOpenFaces(pos);
    }

    public void reset() {
        for (ColumnRender<T> column : this.columns.values()) {
            column.delete();
        }

        this.columns.clear();
        this.visibleChunks.clear();
        this.drawableChunks.clear();
        this.visibleBlockEntities.clear();
        this.unloadQueue.clear();
    }

    public ObjectList<ChunkRender<T>> getVisibleChunks() {
        return this.visibleChunks;
    }

    public ObjectList<ChunkRender<T>> getDrawableChunks() {
        return this.drawableChunks;
    }

    public boolean cleanup() {
        if (!this.unloadQueue.isEmpty()) {
            for (ColumnRender<T> column : this.unloadQueue) {
                if (!column.isChunkPresent()) {
                    this.unloadColumn(column);
                }
            }

            this.unloadQueue.clear();

            return true;
        }

        return false;
    }

    public Collection<BlockEntity> getVisibleBlockEntities() {
        return this.visibleBlockEntities;
    }

    @Override
    public void onChunkAdded(int x, int z) {
        this.loadChunk(x, z);
    }

    @Override
    public void onChunkRemoved(int x, int z) {
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

    private ChunkRender<T> createChunkRender(ColumnRender<T> column, int x, int y, int z) {
        return new ChunkRender<>(column, x, y, z, this.chunkRenderer.createRenderState());
    }

    private void unloadColumn(ColumnRender<T> column) {
        column.delete();

        for (Direction dir : DirectionUtil.HORIZONTAL_DIRECTIONS) {
            ColumnRender<T> adj = column.getNeighbor(dir);

            if (adj != null) {
                adj.setNeighbor(dir.getOpposite(), null);
            }
        }

        this.columns.remove(column.getKey());
    }

    private ColumnRender<T> getRenderColumn(int x, int z) {
        return this.columns.get(ChunkPos.toLong(x, z));
    }

    public void renderLayer(MatrixStack matrixStack, BlockRenderPass layerType, double x, double y, double z) {
        if (!this.renderedLayers.add(layerType)) {
            return;
        }

        boolean notTranslucent = !layerType.isTranslucent();

        ObjectList<ChunkRender<T>> list = this.getDrawableChunks();
        ObjectListIterator<ChunkRender<T>> it = list.listIterator(notTranslucent ? 0 : list.size());

        this.chunkRenderer.begin(matrixStack);

        boolean needManualTicking = SodiumClientMod.options().performance.animateOnlyVisibleTextures;

        while (true) {
            if (notTranslucent) {
                if (!it.hasNext()) {
                    break;
                }
            } else if (!it.hasPrevious()) {
                break;
            }

            ChunkRender<T> render = notTranslucent ? it.next() : it.previous();

            if (needManualTicking) {
                render.tickTextures();
            }

            this.chunkRenderer.render(render, layerType, matrixStack, x, y, z);
        }

        this.chunkRenderer.end(matrixStack);
    }

    public void onFrameChanged() {
        this.renderedLayers.clear();
    }

    public boolean isChunkVisible(int x, int y, int z) {
        ChunkRender<T> render = this.getRender(x, y, z);

        return render != null && render.getLastVisibleFrame() == this.lastFrameUpdated;
    }
}
