package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraph;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkSlice;
import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;

import java.util.concurrent.CompletableFuture;

public class ChunkRender<T extends ChunkRenderData> {
    private final ChunkBuilder builder;

    @SuppressWarnings("unchecked")
    private final ChunkRender<T>[] adjacent = new ChunkRender[6];
    private final ColumnRender<T> column;

    private final BlockPos.Mutable origin;
    private final int chunkX, chunkY, chunkZ;

    private final T renderData;

    private final Box boundingBox;

    private ChunkMeshInfo meshInfo = ChunkMeshInfo.ABSENT;
    private CompletableFuture<Void> rebuildTask = null;

    private volatile boolean needsRebuild;
    private volatile boolean needsImportantRebuild;

    public Direction direction;

    public int rebuildFrame;
    public byte cullingState;

    public ChunkRender(ChunkBuilder builder, T renderData, ColumnRender<T> column, int chunkX, int chunkY, int chunkZ) {
        this.builder = builder;
        this.renderData = renderData;
        this.column = column;

        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;

        int x = this.chunkX << 4;
        int y = this.chunkY << 4;
        int z = this.chunkZ << 4;

        this.origin = new BlockPos.Mutable(x, y, z);
        this.boundingBox = new Box(x, y, z, x + 16.0, y + 16.0, z + 16.0);

        this.needsRebuild = true;
        this.rebuildFrame = -1;
    }

    public void cancelRebuildTask() {
        this.finishRebuild();

        if (this.rebuildTask != null) {
            this.rebuildTask.cancel(false);
            this.rebuildTask = null;
        }
    }

    public ChunkRender<T> getAdjacent(ChunkGraph<T> graph, Direction dir) {
        ChunkRender<T> adj = this.adjacent[dir.ordinal()];

        if (adj == null) {
            adj = this.adjacent[dir.ordinal()] = graph.getOrCreateRender(this.chunkX + dir.getOffsetX(), this.chunkY + dir.getOffsetY(), this.chunkZ + dir.getOffsetZ());
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

    public void deleteData() {
        this.cancelRebuildTask();

        this.renderData.clearData();
        this.meshInfo = ChunkMeshInfo.ABSENT;
    }

    public boolean hasChunkNeighbors(ChunkGraph<T> graph) {
        return this.isNeighborPresent(graph, Direction.WEST) && this.isNeighborPresent(graph, Direction.NORTH) &&
                this.isNeighborPresent(graph, Direction.EAST) && this.isNeighborPresent(graph, Direction.SOUTH);
    }

    private boolean isNeighborPresent(ChunkGraph<T> graph, Direction dir) {
        ChunkRender<T> render = this.getAdjacent(graph, dir);

        return render == null || render.isChunkPresent();
    }

    public void rebuild() {
        this.cancelRebuildTask();

        this.builder.schedule(createRebuildTask(this.builder, this))
                .thenAccept(this.builder::enqueueUpload);
    }

    public CompletableFuture<ChunkRenderUploadTask> rebuildImmediately() {
        this.cancelRebuildTask();

        return this.builder.schedule(createRebuildTask(this.builder, this));
    }

    public void scheduleRebuild(boolean important) {
        this.needsImportantRebuild = important;
        this.needsRebuild = true;
    }

    public void upload(ChunkMeshInfo meshInfo) {
        this.renderData.uploadData(meshInfo);
        this.meshInfo = meshInfo;
    }

    public void finishRebuild() {
        this.needsRebuild = false;
        this.needsImportantRebuild = false;
    }

    public boolean isEmpty() {
        return this.meshInfo.isEmpty();
    }

    public void updateCullingState(byte parent, Direction from) {
        this.cullingState = (byte) (parent | (1 << from.ordinal()));
    }

    public boolean canCull(Direction from) {
        return (this.cullingState & 1 << from.ordinal()) > 0;
    }

    public void resetGraphState() {
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

    public boolean isChunkPresent() {
        return this.column.isChunkPresent();
    }

    public long getPositionKey() {
        return ChunkSectionPos.asLong(this.chunkX, this.chunkY, this.chunkZ);
    }

    private static ChunkRenderBuildTask createRebuildTask(ChunkBuilder builder, ChunkRender<?> render) {
        ChunkSlice slice = ChunkSlice.tryCreate(builder.getWorld(), render.getChunkPos());

        if (slice == null) {
            return new ChunkRenderEmptyBuildTask(render);
        }

        return new ChunkRenderRebuildTask(builder, render, slice);
    }

    private ChunkSectionPos getChunkPos() {
        return ChunkSectionPos.from(this.chunkX, this.chunkY, this.chunkZ);
    }

    public ColumnRender<T> getColumn() {
        return this.column;
    }

    public boolean isVisible(Frustum frustum, int frame) {
        return this.getColumn().isVisible(frustum, frame) && frustum.isVisible(this.getBoundingBox());
    }
}
