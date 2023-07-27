package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import me.jellysquid.mods.sodium.client.render.chunk.graph.VisibilityEncoding;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import me.jellysquid.mods.sodium.client.util.DirectionUtil;
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
    private final int chunkX, chunkY, chunkZ;

    private final int sectionIndex;
    private final int sectionCoord;

    private final RenderRegion region;
    private final RenderSection[] adjacent = new RenderSection[DirectionUtil.ALL_DIRECTIONS.length];

    private BuiltSectionInfo data = BuiltSectionInfo.ABSENT;
    private CompletableFuture<?> rebuildTask = null;

    private ChunkUpdateType pendingUpdate;

    private boolean disposed;

    private int lastAcceptedBuildTime = -1;

    private int flags;

    private int lastVisibleFrame = -1;

    private long visibilityData;

    private int incomingDirections;

    public RenderSection(RenderRegion region, SodiumWorldRenderer worldRenderer, int chunkX, int chunkY, int chunkZ) {
        this.worldRenderer = worldRenderer;

        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;

        int rX = this.getChunkX() & (RenderRegion.REGION_WIDTH - 1);
        int rY = this.getChunkY() & (RenderRegion.REGION_HEIGHT - 1);
        int rZ = this.getChunkZ() & (RenderRegion.REGION_LENGTH - 1);

        this.sectionCoord = (rX << 5 | rY << 3 | rZ << 0) & 0xFF;
        this.sectionIndex = LocalSectionIndex.pack(rX, rY, rZ);

        this.region = region;

        this.visibilityData = VisibilityEncoding.DEFAULT;
    }


    public RenderSection getAdjacent(int direction) {
        return this.adjacent[direction];
    }

    public void setAdjacentNode(int direction, RenderSection node) {
        this.adjacent[direction] = node;
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

        this.disposed = true;
    }

    public void setData(BuiltSectionInfo info) {
        if (info == null) {
            throw new NullPointerException("Mesh information must not be null");
        }

        this.worldRenderer.onChunkRenderUpdated(this.chunkX, this.chunkY, this.chunkZ, this.data, info);
        this.data = info;

        this.flags = info.getFlags();
    }

    public int getFlags() {
        return this.flags;
    }

    /**
     * Returns the chunk section position which this render refers to in the world.
     */
    public ChunkSectionPos getChunkPos() {
        return ChunkSectionPos.from(this.chunkX, this.chunkY, this.chunkZ);
    }

    /**
     * Ensures that all resources attached to the given chunk render are "ticked" forward.
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

    /**
     * @return The squared distance from the center of this chunk in the world to the center of the block position
     * given by {@param pos}
     */
    public float getSquaredDistance(BlockPos pos) {
        return this.getSquaredDistance(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f);
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
    private int getCenterX() {
        return this.getOriginX() + 8;
    }

    /**
     * @return The y-coordinate of the center position of this chunk render
     */
    private int getCenterY() {
        return this.getOriginY() + 8;
    }

    /**
     * @return The z-coordinate of the center position of this chunk render
     */
    private int getCenterZ() {
        return this.getOriginZ() + 8;
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
        if (this.rebuildTask != null) {
            this.rebuildTask.cancel(false);
            this.rebuildTask = null;
        }

        this.rebuildTask = task;
        this.pendingUpdate = null;
    }

    public boolean isBuilt() {
        return this.data != BuiltSectionInfo.ABSENT;
    }

    public boolean canAcceptBuildResults(ChunkBuildResult result) {
        return !this.isDisposed() && result.buildTime > this.lastAcceptedBuildTime;
    }

    public void onBuildFinished(ChunkBuildResult result) {
        this.setData(result.info);
        this.lastAcceptedBuildTime = result.buildTime;
    }

    public int getSectionIndex() {
        return this.sectionIndex;
    }

    public RenderRegion getRegion() {
        return this.region;
    }

    public void setLastVisibleFrame(int frame) {
        this.lastVisibleFrame = frame;
    }

    public int getLastVisibleFrame() {
        return this.lastVisibleFrame;
    }

    public void setOcclusionData(ChunkOcclusionData occlusionData) {
        this.visibilityData = VisibilityEncoding.encode(occlusionData);
    }

    public int getLocalCoord() {
        return this.sectionCoord;
    }

    public long getVisibilityData() {
        return this.visibilityData;
    }

    public int getIncomingDirections() {
        return this.incomingDirections;
    }

    public void addIncomingDirections(int directions) {
        this.incomingDirections |= directions;
    }

    public void setIncomingDirections(int directions) {
        this.incomingDirections = directions;
    }
}
