package net.caffeinemc.sodium.render.chunk;

import net.caffeinemc.sodium.render.SodiumWorldRenderer;
import net.caffeinemc.sodium.render.chunk.compile.tasks.TerrainBuildResult;
import net.caffeinemc.sodium.render.chunk.state.*;
import net.caffeinemc.sodium.render.chunk.region.RenderRegion;
import net.caffeinemc.sodium.render.texture.SpriteUtil;
import net.caffeinemc.sodium.util.DirectionUtil;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;

import java.util.concurrent.CompletableFuture;

/**
 * The render state object for a chunk section. This contains all the graphics state for each render pass along with
 * data about the render in the chunk visibility graph.
 */
public class RenderSection {
    private final SodiumWorldRenderer worldRenderer;

    private final int sectionId;

    private final int chunkX, chunkY, chunkZ;
    private final float originX, originY, originZ;

    private final RenderRegion region;

    private ChunkRenderData data = ChunkRenderData.ABSENT;
    private CompletableFuture<?> rebuildTask = null;

    private ChunkUpdateType pendingUpdate;
    private UploadedChunkGeometry uploadedGeometry;

    private boolean disposed;

    private int lastAcceptedBuildTime = -1;

    private long visibilityBits;

    public RenderSection(SodiumWorldRenderer worldRenderer, int chunkX, int chunkY, int chunkZ, RenderRegion region, int sectionId) {
        this.worldRenderer = worldRenderer;
        this.region = region;

        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;

        this.originX = ChunkSectionPos.getBlockCoord(this.chunkX) + 8;
        this.originY = ChunkSectionPos.getBlockCoord(this.chunkY) + 8;
        this.originZ = ChunkSectionPos.getBlockCoord(this.chunkZ) + 8;

        this.sectionId = sectionId;
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
        this.setData(ChunkRenderData.ABSENT);
        this.deleteGeometry();

        this.disposed = true;
    }

    public void setData(ChunkRenderData info) {
        if (info == null) {
            throw new NullPointerException("Mesh information must not be null");
        }

        this.worldRenderer.onChunkRenderUpdated(this.chunkX, this.chunkY, this.chunkZ, this.data, info);
        this.data = info;
    }

    /**
     * @return True if the chunk render contains no data, otherwise false
     */
    public boolean isEmpty() {
        return this.data.isEmpty();
    }

    /**
     * Returns the chunk section position which this render refers to in the world.
     */
    public ChunkSectionPos getChunkPos() {
        return ChunkSectionPos.from(this.chunkX, this.chunkY, this.chunkZ);
    }

    /**
     * Ensures that all resources attached to the given chunk render are "ticked" forward. This should be called every
     * time before this render is drawn if {@link ChunkRenderData#isTickable()} is true.
     */
    public void tick() {
        for (Sprite sprite : this.data.getAnimatedSprites()) {
            SpriteUtil.markSpriteActive(sprite);
        }
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

    public ChunkRenderBounds getBounds() {
        return this.data.getBounds();
    }

    public RenderRegion getRegion() {
        return this.region;
    }

    public boolean isDisposed() {
        return this.disposed;
    }

    @Override
    public String toString() {
        return String.format("RenderChunk{chunkX=%d, chunkY=%d, chunkZ=%d}",
                this.chunkX, this.chunkY, this.chunkZ);
    }

    public void setOcclusionData(ChunkOcclusionData occlusionData) {
        long visBits = 0;

        for (var from : DirectionUtil.ALL_DIRECTIONS) {
            for (var to : DirectionUtil.ALL_DIRECTIONS) {
                if (occlusionData == null || occlusionData.isVisibleThrough(from, to)) {
                    visBits |= (1L << ((from.ordinal() << 3) + to.ordinal()));
                }
            }
        }

        this.visibilityBits = visBits;
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
        if (this.rebuildTask != null) {
            this.rebuildTask.cancel(false);
            this.rebuildTask = null;
        }

        this.rebuildTask = task;
        this.pendingUpdate = null;
    }

    public boolean isBuilt() {
        return this.data != ChunkRenderData.ABSENT;
    }

    public boolean canAcceptBuildResults(TerrainBuildResult result) {
        return !this.isDisposed() && result.buildTime() > this.lastAcceptedBuildTime;
    }

    public void onBuildFinished(TerrainBuildResult result) {
        this.setData(result.data());
        this.lastAcceptedBuildTime = result.buildTime();
    }

    public void deleteGeometry() {
        if (this.uploadedGeometry != null) {
            this.uploadedGeometry.delete();
            this.uploadedGeometry = null;
        }
    }

    public void updateGeometry(UploadedChunkGeometry geometry) {
        this.deleteGeometry();
        this.uploadedGeometry = geometry;
    }

    public UploadedChunkGeometry getGeometry() {
        return this.uploadedGeometry;
    }

    public int id() {
        return this.sectionId;
    }

    public boolean isVisibleThrough(int incoming, int outgoing) {
        return ((this.visibilityBits & (1L << ((incoming << 3) + outgoing))) != 0L);
    }

    public float getDistance(float x, float y, float z) {
        float xDist = x - this.originX;
        float yDist = y - this.originY;
        float zDist = z - this.originZ;

        return (xDist * xDist) + (yDist * yDist) + (zDist * zDist);
    }

    public float getDistance(float x, float z) {
        float xDist = x - this.originX;
        float zDist = z - this.originZ;

        return (xDist * xDist) + (zDist * zDist);
    }
}
