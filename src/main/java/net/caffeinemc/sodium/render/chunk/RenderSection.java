package net.caffeinemc.sodium.render.chunk;

import java.util.Objects;
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
    private final long regionKey;
    private RenderRegion region;

    private final int sectionX, sectionY, sectionZ;
    private final double centerX, centerY, centerZ;

    private ChunkRenderData data = ChunkRenderData.ABSENT;
    private CompletableFuture<?> rebuildTask = null;

    private ChunkUpdateType pendingUpdate;
    private long uploadedGeometrySegment = BufferSegment.INVALID;

    private boolean disposed;

    private int lastAcceptedBuildTime = -1;
    private int flags;

    public RenderSection(int sectionX, int sectionY, int sectionZ) {
        this.sectionX = sectionX;
        this.sectionY = sectionY;
        this.sectionZ = sectionZ;
        
        this.centerX = ChunkSectionPos.getBlockCoord(this.sectionX) + 8.0;
        this.centerY = ChunkSectionPos.getBlockCoord(this.sectionY) + 8.0;
        this.centerZ = ChunkSectionPos.getBlockCoord(this.sectionZ) + 8.0;
        
        this.regionKey = RenderRegion.getRegionCoord(this.sectionX, this.sectionY, this.sectionZ);
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
        this.ensureGeometryDeleted();
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
        return ChunkSectionPos.from(this.sectionX, this.sectionY, this.sectionZ);
    }

    public int getSectionX() {
        return this.sectionX;
    }

    public int getSectionY() {
        return this.sectionY;
    }

    public int getSectionZ() {
        return this.sectionZ;
    }

    public boolean isDisposed() {
        return this.disposed;
    }

    @Override
    public String toString() {
        return String.format("RenderChunk{chunkX=%d, chunkY=%d, chunkZ=%d}",
                             this.sectionX, this.sectionY, this.sectionZ
        );
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

    public void ensureGeometryDeleted() {
        long uploadedGeometrySegment = this.uploadedGeometrySegment;
        
        if (uploadedGeometrySegment != BufferSegment.INVALID) {
            this.region.removeSection(this);
            this.uploadedGeometrySegment = BufferSegment.INVALID;
            this.region = null;
        }
    }
    
    /**
     * Make sure to call {@link #ensureGeometryDeleted()} before calling this!
     */
    public void setGeometry(RenderRegion region, long bufferSegment) {
        this.setBufferSegment(bufferSegment);
        this.region = region;
    }
    
    public void setBufferSegment(long bufferSegment) {
        if (bufferSegment == BufferSegment.INVALID) {
            throw new IllegalArgumentException("Segment cannot be invalid");
        }
        this.uploadedGeometrySegment = bufferSegment;
    }
    
    public long getUploadedGeometrySegment() {
        return this.uploadedGeometrySegment;
    }

    public double getDistanceSq(double x, double y, double z) {
        double xDist = x - this.centerX;
        double yDist = y - this.centerY;
        double zDist = z - this.centerZ;

        return (xDist * xDist) + (yDist * yDist) + (zDist * zDist);
    }

    public double getDistanceSq(double x, double z) {
        double xDist = x - this.centerX;
        double zDist = z - this.centerZ;

        return (xDist * xDist) + (zDist * zDist);
    }

    public long getRegionKey() {
        return this.regionKey;
    }

    public RenderRegion getRegion() {
        return this.region;
    }

    public boolean isWithinFrustum(Frustum frustum) {
        return frustum.containsBox(
                (float) (this.centerX - 8.0),
                (float) (this.centerY - 8.0),
                (float) (this.centerZ - 8.0),
                (float) (this.centerX + 8.0),
                (float) (this.centerY + 8.0),
                (float) (this.centerZ + 8.0)
        );
    }

    public int getFlags() {
        return this.flags;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        RenderSection section = (RenderSection) o;
        return this.sectionX == section.sectionX &&
               this.sectionY == section.sectionY &&
               this.sectionZ == section.sectionZ;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(this.sectionX, this.sectionY, this.sectionZ);
    }
}
