package net.caffeinemc.sodium.render.chunk;

import java.util.concurrent.CompletableFuture;
import net.caffeinemc.sodium.interop.vanilla.math.frustum.Frustum;
import net.caffeinemc.sodium.render.buffer.arena.BufferSegment;
import net.caffeinemc.sodium.render.chunk.region.RenderRegion;
import net.caffeinemc.sodium.render.chunk.state.ChunkRenderData;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * The render state object for a chunk section. This contains all the graphics state for each render pass along with
 * data about the render in the chunk visibility graph.
 */
public class RenderSection {
    private final int id;

    private final long regionKey;
    private RenderRegion region;

    private final int chunkX, chunkY, chunkZ;
    private final double originX, originY, originZ;

    private ChunkRenderData data = ChunkRenderData.ABSENT;
    private CompletableFuture<?> rebuildTask = null;

    private ChunkUpdateType pendingUpdate;
    private BufferSegment uploadedGeometrySegment;

    private boolean disposed;

    private int lastAcceptedBuildTime = -1;
    private int flags;

    public RenderSection(int chunkX, int chunkY, int chunkZ, int id) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;

        this.originX = ChunkSectionPos.getBlockCoord(this.chunkX) + 8;
        this.originY = ChunkSectionPos.getBlockCoord(this.chunkY) + 8;
        this.originZ = ChunkSectionPos.getBlockCoord(this.chunkZ) + 8;

        this.id = id;
        this.regionKey = RenderRegion.getRegionCoord(this.chunkX, this.chunkY, this.chunkZ);
    }

    /**
     * Cancels any pending tasks to rebuild the chunk. If the result of any pending tasks has not been processed yet,
     * those will also be discarded when processing finally happens.
     */
    public void cancelRebuildTask() {
        if (this.rebuildTask != null) {
            this.rebuildTask.cancel(false);
            this.rebuildTask = null;
        }
    }

    public ChunkRenderData getData() {
        return this.data;
    }

    /**
     * Deletes all data attached to this render and drops any pending tasks. This should be used when the render falls
     * out of view or otherwise needs to be destroyed. After the render has been destroyed, the object can no longer
     * be used.
     */
    public void delete() {
        this.cancelRebuildTask();
        this.deleteGeometry();

        this.disposed = true;
    }

    public void setData(ChunkRenderData data) {
        if (data == null) {
            throw new NullPointerException("Mesh information must not be null");
        }

        this.data = data;
        this.flags = data.getFlags();
    }

    /**
     * Returns the chunk section position which this render refers to in the world.
     */
    public ChunkSectionPos getChunkPos() {
        return ChunkSectionPos.from(this.chunkX, this.chunkY, this.chunkZ);
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

    public boolean isDisposed() {
        return this.disposed;
    }

    @Override
    public String toString() {
        return String.format("RenderChunk{chunkX=%d, chunkY=%d, chunkZ=%d}",
                this.chunkX, this.chunkY, this.chunkZ);
    }

    public ChunkUpdateType getPendingUpdate() {
        return this.pendingUpdate;
    }

    public void markForUpdate(ChunkUpdateType type) {
        if (this.pendingUpdate == null || type.ordinal() > this.pendingUpdate.ordinal()) {
            this.pendingUpdate = type;
        }
    }

    public void onBuildSubmitted(CompletableFuture<?> task) {
        this.cancelRebuildTask();

        this.rebuildTask = task;
        this.pendingUpdate = null;
    }

    public boolean isBuilt() {
        return this.data != ChunkRenderData.ABSENT;
    }

    public int getLastAcceptedBuildTime() {
        return this.lastAcceptedBuildTime;
    }

    public void setLastAcceptedBuildTime(int time) {
        this.lastAcceptedBuildTime = time;
    }

    public void deleteGeometry() {
        if (this.uploadedGeometrySegment != null) {
            this.uploadedGeometrySegment.delete();
            this.uploadedGeometrySegment = null;

            this.region = null;
        }
    }

    public void updateGeometry(RenderRegion region, BufferSegment segment) {
        this.deleteGeometry();
        this.uploadedGeometrySegment = segment;
        this.region = region;
    }
    
    public BufferSegment getUploadedGeometrySegment() {
        return this.uploadedGeometrySegment;
    }
    
    public int id() {
        return this.id;
    }

    public double getDistance(double x, double y, double z) {
        double xDist = x - this.originX;
        double yDist = y - this.originY;
        double zDist = z - this.originZ;

        return (xDist * xDist) + (yDist * yDist) + (zDist * zDist);
    }

    public double getDistance(double x, double z) {
        double xDist = x - this.originX;
        double zDist = z - this.originZ;

        return (xDist * xDist) + (zDist * zDist);
    }

    public long getRegionKey() {
        return this.regionKey;
    }

    public RenderRegion getRegion() {
        return this.region;
    }

    public boolean isWithinFrustum(Frustum frustum) {
        return frustum.isBoxVisible(
                (float) (this.originX - 8.0),
                (float) (this.originY - 8.0),
                (float) (this.originZ - 8.0),
                (float) (this.originX + 8.0),
                (float) (this.originY + 8.0),
                (float) (this.originZ + 8.0)
        );
    }

    public int getFlags() {
        return this.flags;
    }
}
