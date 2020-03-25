package me.jellysquid.mods.sodium.client.render.chunk.compile;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderer;
import me.jellysquid.mods.sodium.client.render.vertex.BufferUploadData;
import net.minecraft.client.render.RenderLayer;
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

    private ChunkMeshInfo meshInfo = ChunkMeshInfo.empty();
    private CompletableFuture<Void> rebuildTask = null;

    private volatile boolean needsRebuild;
    private volatile boolean needsImportantRebuild;

    private AtomicBoolean invalid = new AtomicBoolean(false);

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

        this.meshInfo = ChunkMeshInfo.empty();

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

    public void upload(ChunkMeshInfo meshInfo, Object2ObjectMap<RenderLayer, BufferUploadData> uploads) {
        this.meshInfo = meshInfo;

        T data = this.renderData;

        if (data == null) {
            if (uploads.isEmpty()) {
                return;
            }

            data = this.renderData = this.chunkRenderer.createRenderData();
        }

        if (uploads.isEmpty()) {
            data.deleteMeshes();
        } else {
            data.uploadMeshes(uploads);
        }
    }

    public void finishRebuild() {
        this.needsRebuild = false;
        this.needsImportantRebuild = false;
    }

    public boolean isEmpty() {
        return this.renderData == null;
    }
}
