package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChunkRender<T extends ChunkRenderData> {
    private final ChunkBuilder builder;
    private final BlockPos.Mutable origin = new BlockPos.Mutable();
    private final ChunkRenderer<T> chunkRenderer;
    private final int chunkX, chunkY, chunkZ;

    private Box boundingBox;
    private T renderData;

    private ChunkMeshInfo meshInfo = ChunkMeshInfo.ABSENT;
    private CompletableFuture<Void> rebuildTask = null;

    private volatile boolean needsRebuild;
    private volatile boolean needsImportantRebuild;

    private AtomicBoolean invalid = new AtomicBoolean(false);

    public Direction direction;

    public int propagationLevel;
    public int rebuildFrame;
    public byte cullingState;

    public ChunkRender(ChunkBuilder builder, ChunkRenderer<T> chunkRenderer, BlockPos origin) {
        this.builder = builder;
        this.chunkRenderer = chunkRenderer;

        int x = origin.getX();
        int y = origin.getY();
        int z = origin.getZ();

        this.origin.set(x, y, z);
        this.boundingBox = new Box(x, y, z, x + 16.0, y + 16.0, z + 16.0);

        this.chunkX = x >> 4;
        this.chunkY = y >> 4;
        this.chunkZ = z >> 4;

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

    public ChunkRenderRebuildTask createRebuildTask() {
        this.cancelRebuildTask();

        return new ChunkRenderRebuildTask(this.builder, this);
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

    public CompletableFuture<ChunkRenderUploadTask> rebuildImmediately() {
        return this.builder.schedule(this.createRebuildTask());
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

    public void delete() {
        this.invalid.set(true);

        this.cancelRebuildTask();

        this.meshInfo = ChunkMeshInfo.ABSENT;

        if (this.renderData != null) {
            this.renderData.destroy();
            this.renderData = null;
        }
    }

    public boolean hasNeighbors() {
        return this.isChunkPresent(Direction.WEST) && this.isChunkPresent(Direction.NORTH) && this.isChunkPresent(Direction.EAST) && this.isChunkPresent(Direction.SOUTH);
    }

    private boolean isChunkPresent(Direction dir) {
        return this.builder.getWorld().getChunk(this.chunkX + dir.getOffsetX(), this.chunkZ + dir.getOffsetZ(), ChunkStatus.FULL, false) != null;
    }

    public void rebuild() {
        this.builder.schedule(this.createRebuildTask())
                .thenAccept(this.builder::addUploadTask);
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
                this.renderData.destroy();
                this.renderData = null;
            }
        } else {
            if (this.renderData == null) {
                this.renderData = this.chunkRenderer.createRenderData();
            }

            this.renderData.uploadChunk(meshInfo);

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
        this.propagationLevel = 0;
        this.cullingState = 0;
    }

    public void setRebuildFrame(int frame) {
        this.rebuildFrame = frame;
    }

    public void setPropagationLevel(int level) {
        this.propagationLevel = level;
    }

    public void setDirection(Direction dir) {
        this.direction = dir;
    }

    public int getRebuildFrame() {
        return this.rebuildFrame;
    }

    public void setCullingState(byte i) {
        this.cullingState = i;
    }
}
