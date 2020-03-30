package me.jellysquid.mods.sodium.client.render.chunk;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkRender;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ColumnRender;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListener;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;

public class ChunkGraph<T extends ChunkRenderData> implements ChunkStatusListener {
    private final Long2ObjectOpenHashMap<ColumnRender<T>> columns = new Long2ObjectOpenHashMap<>();

    private final ObjectList<ChunkRender<T>> unloadQueue = new ObjectArrayList<>();
    private final ObjectList<ChunkRender<T>> visibleChunks = new ObjectArrayList<>();
    private final ObjectList<ChunkRender<T>> drawableChunks = new ObjectArrayList<>();

    private final ObjectList<BlockEntity> visibleBlockEntities = new ObjectArrayList<>();

    private final ObjectArrayFIFOQueue<ChunkRender<T>> iterationQueue = new ObjectArrayFIFOQueue<>();

    private final ChunkRenderManager<T> renderManager;
    private final World world;

    private int minChunkX, minChunkZ, maxChunkX, maxChunkZ;
    private int renderDistance;

    public ChunkGraph(ChunkRenderManager<T> renderManager, World world, int renderDistance) {
        this.renderManager = renderManager;
        this.world = world;
        this.renderDistance = renderDistance;
    }

    public void calculateVisible(Camera camera, Vec3d cameraPos, BlockPos blockPos, int frame, Frustum frustum, boolean spectator) {
        int maxDistBlocks = this.renderDistance * 16;

        this.minChunkX = MathHelper.floor(cameraPos.x - maxDistBlocks) >> 4;
        this.minChunkZ = MathHelper.floor(cameraPos.z - maxDistBlocks) >> 4;

        this.maxChunkX = MathHelper.floor(cameraPos.x + maxDistBlocks) >> 4;
        this.maxChunkZ = MathHelper.floor(cameraPos.z + maxDistBlocks) >> 4;

        this.visibleChunks.clear();
        this.drawableChunks.clear();
        this.visibleBlockEntities.clear();

        boolean cull = this.init(blockPos, camera, cameraPos, frustum, frame, spectator);

        SodiumGameOptions options = SodiumClientMod.options();

        boolean fogCulling = cull && this.renderDistance > 4 && options.quality.enableFog && options.performance.useFogChunkCulling;
        int maxChunkDistance = (this.renderDistance * 16) + 16;

        while (!this.iterationQueue.isEmpty()) {
            ChunkRender<T> render = this.iterationQueue.dequeue();

            this.markVisible(render);

            if (fogCulling && !render.getOrigin().isWithinDistance(cameraPos, maxChunkDistance)) {
                continue;
            }

            this.addNeighbors(render, cull, frustum, frame);
        }
    }

    private void markVisible(ChunkRender<T> render) {
        this.visibleChunks.add(render);

        if (!render.isEmpty()) {
            this.drawableChunks.add(render);
        }

        Collection<BlockEntity> blockEntities = render.getMeshInfo().getBlockEntities();

        if (!blockEntities.isEmpty()) {
            this.visibleBlockEntities.addAll(blockEntities);
        }
    }

    private void addNeighbors(ChunkRender<T> render, boolean cull, Frustum frustum, int frame) {
        for (Direction adjDir : DirectionUtil.ALL_DIRECTIONS) {
            ChunkRender<T> adjRender = this.getAdjacentRender(render, adjDir);

            if (adjRender == null || adjRender.getRebuildFrame() == frame) {
                continue;
            }

            if (cull && !this.isVisible(render, adjRender, adjDir, frustum, frame)) {
                continue;
            }

            if (!adjRender.hasChunkNeighbors(this)) {
                continue;
            }

            adjRender.setDirection(adjDir);
            adjRender.setRebuildFrame(frame);
            adjRender.updateCullingState(render.cullingState, adjDir);

            this.iterationQueue.enqueue(adjRender);
        }
    }

    private boolean isVisible(ChunkRender<T> render, ChunkRender<T> adjRender, Direction dir, Frustum frustum, int frame) {
        if (render.canCull(dir.getOpposite())) {
            return false;
        }

        if (render.direction != null && !render.isVisibleThrough(render.direction.getOpposite(), dir)) {
            return false;
        }

        return adjRender.isVisible(frustum, frame);
    }

    private boolean init(BlockPos blockPos, Camera camera, Vec3d cameraPos, Frustum frustum, int frame, boolean spectator) {
        MinecraftClient client = MinecraftClient.getInstance();
        ObjectArrayFIFOQueue<ChunkRender<T>> queue = this.iterationQueue;

        boolean cull = client.chunkCullingEnabled;

        ChunkRender<T> node = this.getOrCreateRender(blockPos);

        if (node != null) {
            node.resetGraphState();

            // Player is within bounds and inside a node
            Set<Direction> openFaces = this.getOpenChunkFaces(blockPos);

            if (openFaces.size() == 1) {
                Vector3f vector3f = camera.getHorizontalPlane();
                Direction direction = Direction.getFacing(vector3f.getX(), vector3f.getY(), vector3f.getZ()).getOpposite();

                openFaces.remove(direction);
            }

            if (openFaces.isEmpty() && !spectator) {
                this.visibleChunks.add(node);
            } else {
                if (spectator && this.world.getBlockState(blockPos).isFullOpaque(this.world, blockPos)) {
                    cull = false;
                }

                node.setRebuildFrame(frame);
                queue.enqueue(node);
            }
        } else {
            // Player is out-of-bounds
            int y = blockPos.getY() > 0 ? 248 : 8;

            int x = MathHelper.floor(cameraPos.x / 16.0D) * 16;
            int z = MathHelper.floor(cameraPos.z / 16.0D) * 16;

            List<ChunkRender<T>> list = Lists.newArrayList();

            for (int x2 = -this.renderDistance; x2 <= this.renderDistance; ++x2) {
                for (int z2 = -this.renderDistance; z2 <= this.renderDistance; ++z2) {
                    ChunkRender<T> chunk = this.getOrCreateRender(new BlockPos(x + (x2 << 4) + 8, y, z + (z2 << 4) + 8));

                    if (chunk==null || !frustum.isVisible(chunk.getBoundingBox())) {
                        continue;
                    }

                    chunk.setRebuildFrame(frame);
                    chunk.resetGraphState();

                    list.add(chunk);
                }
            }

            list.sort(Comparator.comparingDouble(o -> blockPos.getSquaredDistance(o.getOrigin().add(8, 8, 8))));

            for (ChunkRender<T> n : list) {
                queue.enqueue(n);
            }
        }

        return cull;
    }

    public ChunkRender<T> getOrCreateRender(BlockPos pos) {
        return this.getOrCreateRender(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
    }

    public ChunkRender<T> getOrCreateRender(int x, int y, int z) {
        return this.columns.computeIfAbsent(ChunkPos.toLong(x, z), this::createColumn)
                .getOrCreateChunk(y, this.renderManager::createChunkRender);
    }

    public ChunkRender<T> getRender(int x, int y, int z) {
        ColumnRender<T> column = this.columns.get(ChunkPos.toLong(x, z));

        if (column == null) {
            return null;
        }

        return column.getChunk(y);
    }

    private ColumnRender<T> createColumn(long pos) {
        return new ColumnRender<>(this.world, ChunkPos.getPackedX(pos), ChunkPos.getPackedZ(pos));
    }

    private ChunkRender<T> getAdjacentRender(ChunkRender<T> render, Direction direction) {
        int x = render.getChunkX() + direction.getOffsetX();
        int y = render.getChunkY() + direction.getOffsetY();
        int z = render.getChunkZ() + direction.getOffsetZ();

        if (!this.isWithinRenderBounds(x, y, z)) {
            return null;
        }

        return render.getAdjacent(this, direction);
    }

    private boolean isWithinRenderBounds(int x, int y, int z) {
        return y >= 0 && y < 16 && x >= this.minChunkX && x <= this.maxChunkX && z >= this.minChunkZ && z <= this.maxChunkZ;
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
                    mpos.set(x, y, z);

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
            column.deleteData();
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
            for (ChunkRender<T> render : this.unloadQueue) {
                this.removeRenderer(render);
            }

            this.unloadQueue.clear();

            return true;
        }

        return false;
    }

    private void removeRenderer(ChunkRender<T> render) {
        render.deleteData();

        ColumnRender<T> column = render.getColumn();
        column.remove(render);

        if (column.isEmpty()) {
            this.columns.remove(column.getKey());
        }
    }

    public Collection<BlockEntity> getVisibleBlockEntities() {
        return this.visibleBlockEntities;
    }

    @Override
    public void onChunkAdded(int x, int z) {
        ColumnRender<T> column = this.getRenderColumn(x, z);

        if (column != null) {
            column.setChunkPresent(true);
        }
    }

    @Override
    public void onChunkRemoved(int x, int z) {
        ColumnRender<T> column = this.getRenderColumn(x, z);

        if (column != null) {
            column.setChunkPresent(false);
        }
    }

    private ColumnRender<T> getRenderColumn(int x, int z) {
        return this.columns.get(ChunkPos.toLong(x, z));
    }
}
