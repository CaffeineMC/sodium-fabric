package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraph;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkSlice;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChunkRender<T extends ChunkRenderData> {
    private final ChunkGraph<T> graph;
    private final ChunkBuilder builder;
    private final BlockPos.Mutable origin;
    private final ChunkRenderer<T> chunkRenderer;
    private final int chunkX, chunkY, chunkZ;
    private final ChunkRender<T>[] adjacent;
    private final long chunkPosKey;

    private Box boundingBox;
    private T renderData;

    private ChunkMeshInfo meshInfo = ChunkMeshInfo.ABSENT;
    private CompletableFuture<Void> rebuildTask = null;

    private volatile boolean needsRebuild;
    private volatile boolean needsImportantRebuild;

    private AtomicBoolean invalid = new AtomicBoolean(false);

    public Direction direction;

    public int rebuildFrame;
    public byte cullingState;
    private boolean chunkPresent;

    @SuppressWarnings("unchecked")
    public ChunkRender(ChunkGraph<T> graph, ChunkBuilder builder, ChunkRenderer<T> chunkRenderer, long chunkPosKey) {
        this.graph = graph;
        this.builder = builder;
        this.chunkRenderer = chunkRenderer;

        this.chunkPosKey = chunkPosKey;
        this.chunkX = ChunkSectionPos.getX(chunkPosKey);
        this.chunkY = ChunkSectionPos.getY(chunkPosKey);
        this.chunkZ = ChunkSectionPos.getZ(chunkPosKey);

        int x = this.chunkX << 4;
        int y = this.chunkY << 4;
        int z = this.chunkZ << 4;

        this.origin = new BlockPos.Mutable(x, y, z);
        this.boundingBox = new Box(x, y, z, x + 16.0, y + 16.0, z + 16.0);

        this.needsRebuild = true;
        this.rebuildFrame = -1;

        this.adjacent = new ChunkRender[6];
        this.refreshChunk();
    }

    public void cancelRebuildTask() {
        this.finishRebuild();

        if (this.rebuildTask != null) {
            this.rebuildTask.cancel(false);
            this.rebuildTask = null;
        }
    }

    public ChunkRender<T> getAdjacent(Direction dir) {
        ChunkRender<T> adj = this.adjacent[dir.ordinal()];

        if (adj == null) {
            adj = this.adjacent[dir.ordinal()] = this.graph.getOrCreateRender(this.chunkX + dir.getOffsetX(), this.chunkY + dir.getOffsetY(), this.chunkZ + dir.getOffsetZ());
        }

        return adj;
    }

    public BlockPos getOrigin() {
        return this.origin;
    }

    public Box getBoundingBox() {
        return this.boundingBox;
    }

    public ChunkMeshInfo getMeshInfo() {
        return this.meshInfo;
    }

    public boolean needsRebuild() {
        return this.needsRebuild;
    }

    public boolean needsImportantRebuild() {
        return this.needsRebuild && this.needsImportantRebuild;
    }

    public int getChunkX() {
        return this.chunkX;
    }

    public int getChunkY() {
        return this.chunkY;
    }

    public int getChunkZ() {
        return this.chunkZ;
    }

    public boolean isVisibleThrough(Direction from, Direction to) {
        return this.meshInfo.isVisibleThrough(from, to);
    }

    public T getRenderData() {
        return this.renderData;
    }

    public void deleteResources() {
        this.invalid.set(true);

        this.cancelRebuildTask();

        this.meshInfo = ChunkMeshInfo.ABSENT;

        if (this.renderData != null) {
            this.renderData.clearData();
            this.renderData = null;
        }
    }

    public boolean hasNeighbors() {
        return this.isChunkPresent(Direction.WEST) && this.isChunkPresent(Direction.NORTH) &&
                this.isChunkPresent(Direction.EAST) && this.isChunkPresent(Direction.SOUTH);
    }

    private boolean isChunkPresent(Direction dir) {
        ChunkRender<T> render = this.getAdjacent(dir);

        return render == null || render.isChunkPresent();
    }

    public void rebuild() {
        this.cancelRebuildTask();

        this.builder.schedule(createRebuildTask(this.builder, this))
                .thenAccept(this.builder::addUploadTask);
    }

    public CompletableFuture<ChunkRenderUploadTask> rebuildImmediately() {
        this.cancelRebuildTask();

        return this.builder.schedule(createRebuildTask(this.builder, this));
    }

    public void scheduleRebuild(boolean important) {
        this.needsImportantRebuild = important;
        this.needsRebuild = true;
    }

    public boolean isInvalid() {
        return this.invalid.get();
    }

    public void upload(ChunkMeshInfo meshInfo) {
        if (meshInfo.isEmpty()) {
            if (this.renderData != null) {
                this.renderData.clearData();
                this.renderData = null;
            }
        } else {
            if (this.renderData == null) {
                this.renderData = this.chunkRenderer.createRenderData();
            }

            this.renderData.uploadData(meshInfo);

            meshInfo.clearUploads();
        }

        this.meshInfo = meshInfo;
    }

    public void finishRebuild() {
        this.needsRebuild = false;
        this.needsImportantRebuild = false;
    }

    public boolean isEmpty() {
        return this.renderData == null;
    }

    public void updateCullingState(byte parent, Direction from) {
        this.cullingState = (byte) (parent | (1 << from.ordinal()));
    }

    public boolean canCull(Direction from) {
        return (this.cullingState & 1 << from.ordinal()) > 0;
    }

    public void reset() {
        this.direction = null;
        this.cullingState = 0;
    }

    public void setRebuildFrame(int frame) {
        this.rebuildFrame = frame;
    }

    public void setDirection(Direction dir) {
        this.direction = dir;
    }

    public int getRebuildFrame() {
        return this.rebuildFrame;
    }

    public void setChunkPresent(boolean value) {
        this.chunkPresent = value;
    }

    public boolean isChunkPresent() {
        return this.chunkPresent;
    }

    public long getPositionKey() {
        return this.chunkPosKey;
    }

    public void refreshChunk() {
        this.chunkPresent = this.builder.getWorld().isChunkLoaded(this.chunkX, this.chunkZ);
    }

    private static ChunkRenderBuildTask createRebuildTask(ChunkBuilder builder, ChunkRender<?> render) {
        BlockPos origin = render.getOrigin();

        BlockPos from = origin.add(-1, -1, -1);
        BlockPos to = origin.add(16, 16, 16);

        ChunkSlice slice = ChunkSlice.tryCreate(builder.getWorld(), render.getChunkPos());

        if (slice == null) {
            return new ChunkRenderEmptyBuildTask(builder, render);
        }

        return new ChunkRenderRebuildTask(builder, render, slice);
    }

    private ChunkSectionPos getChunkPos() {
        return ChunkSectionPos.from(this.chunkX, this.chunkY, this.chunkZ);
    }
}
