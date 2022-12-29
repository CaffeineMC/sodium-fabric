package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderBounds;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.ChunkSectionPos;

import java.util.concurrent.CompletableFuture;

/**
 * The render state object for a chunk section. This contains all the graphics state for each render pass along with
 * data about the render in the chunk visibility graph.
 */
public class RenderSection {
    private final int chunkX, chunkY, chunkZ;

    private ChunkRenderData data = ChunkRenderData.ABSENT;
    private CompletableFuture<?> rebuildFuture;

    private ChunkUpdateType pendingUpdate;
    private int lastRebuildTime;

    private int flags;
    private boolean disposed;

    private final long regionId;
    private final int chunkId;

    public RenderSection(int chunkX, int chunkY, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;

        int rX = this.getChunkX() & (RenderRegion.REGION_WIDTH - 1);
        int rY = this.getChunkY() & (RenderRegion.REGION_HEIGHT - 1);
        int rZ = this.getChunkZ() & (RenderRegion.REGION_LENGTH - 1);

        this.chunkId = RenderRegion.getChunkIndex(rX, rY, rZ);
        this.regionId = RenderRegion.getRegionKeyForChunk(this.chunkX, this.chunkY, this.chunkZ);
    }

    public ChunkRenderData getData() {
        return this.data;
    }

    public void setData(ChunkRenderData info) {
        if (info == null) {
            throw new NullPointerException("Mesh information must not be null");
        }

        this.data = info;
        this.flags = 0;

        if (!info.getRenderLayers().isEmpty()) {
            this.flags |= ChunkDataFlags.HAS_BLOCK_GEOMETRY;
        }

        if (!info.getAnimatedSprites().isEmpty()) {
            this.flags |= ChunkDataFlags.HAS_ANIMATED_SPRITES;
        }

        if (!info.getBlockEntities().isEmpty()) {
            this.flags |= ChunkDataFlags.HAS_BLOCK_ENTITIES;
        }
    }

    /**
     * Returns the chunk section position which this render refers to in the world.
     */
    public ChunkSectionPos getChunkPos() {
        return ChunkSectionPos.from(this.chunkX, this.chunkY, this.chunkZ);
    }

    /**
     * Ensures that all resources attached to the given chunk render are "ticked" forward. This should be called every
     * time before this render is drawn if the flag {@link ChunkDataFlags#HAS_ANIMATED_SPRITES} is present.
     */
    public void tick() {
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

    public boolean hasFlag(int flag) {
        return (this.flags & flag) != 0;
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

    public boolean isBuilt() {
        return this.data != ChunkRenderData.ABSENT;
    }

    public int getLocalId() {
        return this.chunkId;
    }

    public long getRegionId() {
        return this.regionId;
    }

    public void dispose() {
        this.disposed = true;
    }

    public int getLastRebuildTime() {
        return this.lastRebuildTime;
    }

    public void cancelRebuild() {
        if (this.rebuildFuture != null) {
            this.rebuildFuture.cancel(false);
            this.rebuildFuture = null;
        }
    }

    public void setRebuildFuture(CompletableFuture<?> future, int timestamp) {
        this.rebuildFuture = future;
        this.lastRebuildTime = timestamp;

        this.pendingUpdate = null;
    }

    public void markForUpdate(ChunkUpdateType type) {
        if (this.pendingUpdate == null || type.ordinal() > this.pendingUpdate.ordinal()) {
            this.pendingUpdate = type;
        }
    }

    public void finishRebuild() {
        this.rebuildFuture = null;
    }

    public boolean hasFlags() {
        return this.flags != 0;
    }
}
