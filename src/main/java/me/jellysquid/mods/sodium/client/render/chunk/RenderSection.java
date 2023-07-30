package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import me.jellysquid.mods.sodium.client.render.chunk.graph.GraphNodeFlags;
import me.jellysquid.mods.sodium.client.render.chunk.graph.LocalSectionIndex;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.ChunkSectionPos;

import java.util.concurrent.CompletableFuture;

/**
 * The render state object for a chunk section. This contains all the graphics state for each render pass along with
 * data about the render in the chunk visibility graph.
 */
@Deprecated
public class RenderSection {
    private final int chunkX, chunkY, chunkZ;
    private ChunkUpdateType pendingUpdate;

    @Deprecated(forRemoval = true) // reason: render sections should not have references to their owner
    private final SodiumWorldRenderer worldRenderer;

    private final int chunkCoord;
    private final int chunkIndex;

    public RenderRegion region;

    @Deprecated(forRemoval = true)
    private BuiltSectionInfo data = BuiltSectionInfo.ABSENT;
    private CompletableFuture<?> rebuildTask = null;

    private int lastAcceptedBuildTime = -1;

    public RenderSection(SodiumWorldRenderer worldRenderer, int chunkX, int chunkY, int chunkZ) {
        this.worldRenderer = worldRenderer;

        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;

        int rX = this.getChunkX() & (RenderRegion.REGION_WIDTH - 1);
        int rY = this.getChunkY() & (RenderRegion.REGION_HEIGHT - 1);
        int rZ = this.getChunkZ() & (RenderRegion.REGION_LENGTH - 1);

        this.chunkCoord = (rX << 5 | rY << 3 | rZ << 0) & 0xFF;
        this.chunkIndex = LocalSectionIndex.pack(rX, rY, rZ);
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

    public BuiltSectionInfo getData() {
        return this.data;
    }

    /**
     * Deletes all data attached to this render and drops any pending tasks. This should be used when the render falls
     * out of view or otherwise needs to be destroyed. After the render has been destroyed, the object can no longer
     * be used.
     */
    public void delete() {
        this.cancelRebuildTask();
        this.setData(BuiltSectionInfo.ABSENT);
    }

    public void setData(BuiltSectionInfo info) {
        if (info == null) {
            throw new NullPointerException("Mesh information must not be null");
        }

        this.worldRenderer.onChunkRenderUpdated(this.chunkX, this.chunkY, this.chunkZ, this, this.data, info);
        this.data = info;
    }

    /**
     * Returns the chunk section position which this render refers to in the world.
     */
    public ChunkSectionPos getChunkPos() {
        return ChunkSectionPos.from(this.chunkX, this.chunkY, this.chunkZ);
    }

    /**
     * Ensures that all resources attached to the given chunk render are "ticked" forward. This should be called every
     * time before this render is drawn if {@link RenderSection#getFlags()} contains {@link GraphNodeFlags#HAS_ANIMATED_SPRITES}.
     */
    public void tickAnimatedSprites() {
        for (Sprite sprite : this.data.getAnimatedSprites()) {
            SpriteUtil.markSpriteActive(sprite);
        }
    }

    /**
     * @return The x-coordinate of the origin position of this chunk render
     */
    public int getOriginX() {
        return this.chunkX << 4;
    }

    /**
     * @return The y-coordinate of the origin position of this chunk render
     */
    public int getOriginY() {
        return this.chunkY << 4;
    }

    /**
     * @return The z-coordinate of the origin position of this chunk render
     */
    public int getOriginZ() {
        return this.chunkZ << 4;
    }

    /**
     * @return The squared distance from the center of this chunk in the world to the given position
     */
    public float getSquaredDistance(float x, float y, float z) {
        float xDist = x - this.getCenterX();
        float yDist = y - this.getCenterY();
        float zDist = z - this.getCenterZ();

        return (xDist * xDist) + (yDist * yDist) + (zDist * zDist);
    }

    /**
     * @return The x-coordinate of the center position of this chunk render
     */
    private float getCenterX() {
        return this.getOriginX() + 8.0f;
    }

    /**
     * @return The y-coordinate of the center position of this chunk render
     */
    private float getCenterY() {
        return this.getOriginY() + 8.0f;
    }

    /**
     * @return The z-coordinate of the center position of this chunk render
     */
    private float getCenterZ() {
        return this.getOriginZ() + 8.0f;
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
        }

        this.rebuildTask = task;
        this.pendingUpdate = null;
    }

    public boolean isBuilt() {
        return this.data != BuiltSectionInfo.ABSENT;
    }

    public boolean canAcceptBuildResults(ChunkBuildResult result) {
        return this.region != null && result.buildTime > this.lastAcceptedBuildTime;
    }

    public void onBuildFinished(ChunkBuildResult result) {
        this.setData(result.data);
        this.lastAcceptedBuildTime = result.buildTime;
    }

    public int getLocalSectionCoord() {
        return this.chunkCoord;
    }

    public int getLocalSectionIndex() {
        return this.chunkIndex;
    }

    @Override
    public String toString() {
        return String.format("RenderChunk{chunkX=%d, chunkY=%d, chunkZ=%d}",
                this.chunkX, this.chunkY, this.chunkZ);
    }

    public RenderRegion getRegion() {
        return this.region;
    }
}
