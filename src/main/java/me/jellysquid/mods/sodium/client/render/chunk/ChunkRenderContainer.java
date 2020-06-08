package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.FrustumExtended;
import me.jellysquid.mods.sodium.client.render.backends.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.Vector3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The render state object for a chunk section. This contains all the graphics state for each render pass along with
 * data about the render in the chunk visibility graph.
 */
public class ChunkRenderContainer<T extends ChunkGraphicsState> {
    private final ColumnRender<T> column;
    private final int chunkX, chunkY, chunkZ;

    private T graphicsState;

    private ChunkRenderData data = ChunkRenderData.ABSENT;
    private CompletableFuture<Void> rebuildTask = null;

    private boolean needsRebuild;
    private boolean needsImportantRebuild;

    private int rebuildFrame = -1;
    private int lastVisibleFrame = -1;

    private byte cullingState;
    private byte direction;

    private final float boundsMinX;
    private final float boundsMinY;
    private final float boundsMinZ;

    private final float boundsMaxX;
    private final float boundsMaxY;
    private final float boundsMaxZ;

    public ChunkRenderContainer(ColumnRender<T> column, int chunkX, int chunkY, int chunkZ) {
        this.column = column;

        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;

        int originX = chunkX << 4;
        int originY = chunkY << 4;
        int originZ = chunkZ << 4;

        this.boundsMinX = originX;
        this.boundsMinY = originY;
        this.boundsMinZ = originZ;
        this.boundsMaxX = originX + 16.0f;
        this.boundsMaxY = originY + 16.0f;
        this.boundsMaxZ = originZ + 16.0f;

        if (column.isSectionEmpty(chunkY)) {
            this.setData(ChunkRenderData.EMPTY);
        } else {
            this.scheduleRebuild(false);
        }
    }

    /**
     * Cancels any pending tasks to rebuild the chunk. If the result of any pending tasks has not been processed yet,
     * those will also be discarded when processing finally happens.
     */
    public void cancelRebuildTask() {
        this.needsRebuild = false;
        this.needsImportantRebuild = false;

        if (this.rebuildTask != null) {
            this.rebuildTask.cancel(false);
            this.rebuildTask = null;
        }
    }

    public ChunkRenderData getData() {
        return this.data;
    }

    /**
     * @return True if the render's state is out of date with the world state
     */
    public boolean needsRebuild() {
        return this.needsRebuild;
    }

    /**
     * @return True if the render's rebuild should be performed as blocking
     */
    public boolean needsImportantRebuild() {
        return this.needsImportantRebuild;
    }

    /**
     * The y-coordinate of the chunk section belonging to this render.
     */
    public int getChunkY() {
        return this.chunkY;
    }

    /**
     * @return True if the adjacent chunk can be seen through this one, otherwise false
     */
    public boolean isVisibleThrough(Direction from, Direction to) {
        return this.data.isVisibleThrough(from, to);
    }

    /**
     * Deletes all data attached to this render and drops any pending tasks. This should be used when the render falls
     * out of view or otherwise needs to be destroyed. After the render has been destroyed, the object can no longer
     * be used.
     */
    public void delete() {
        this.cancelRebuildTask();
        this.setData(ChunkRenderData.ABSENT);

        if (this.graphicsState != null) {
            this.graphicsState.delete();
            this.graphicsState = null;
        }
    }

    public void setData(ChunkRenderData info) {
        if (info == null) {
            throw new NullPointerException("Mesh information must not be null");
        }

        this.column.onChunkRenderUpdated(this.data, info);
        this.data = info;
    }

    /**
     * Marks this render as needing an update. Important updates are scheduled as "blocking" and will prevent the next
     * frame from being rendered until the update is performed.
     * @param important True if the update is blocking, otherwise false
     */
    public void scheduleRebuild(boolean important) {
        this.needsImportantRebuild = important;
        this.needsRebuild = true;
    }

    /**
     * @return True if the chunk render contains no data, otherwise false
     */
    public boolean isEmpty() {
        return this.data.isEmpty();
    }

    /**
     * @param parent The culling state of the previous node
     * @param dir The direction in which this node was traversed into from the parent
     */
    public void updateCullingState(byte parent, Direction dir) {
        this.cullingState = (byte) (parent | (1 << dir.ordinal()));
    }

    /**
     * @param dir The direction of the neighbor
     * @return True if this render can cull an adjacent neighbor, otherwise false
     */
    public boolean canCull(Direction dir) {
        return (this.cullingState & 1 << dir.ordinal()) > 0;
    }

    /**
     * Resets the state of this render in the chunk graph. This will be called on the seed node before the breadth-first
     * search is intitiated.
     */
    public void resetGraphState() {
        this.direction = -1;
        this.cullingState = 0;
    }

    /**
     * Sets the frame index which this render was last updated on the chunk graph.
     */
    public void setLastGraphUpdateFrame(int frame) {
        this.rebuildFrame = frame;
    }

    /**
     * Sets the frame index which this render was last visible on after culling.
     */
    public void setLastVisibleFrame(int frame) {
        this.lastVisibleFrame = frame;
    }

    /**
     * Sets the direction in which this node was traversed through on the chunk graph.
     */
    public void setDirection(Direction dir) {
        this.direction = (byte) dir.ordinal();
    }

    /**
     * Returns the last frame index which this render was updated on the chunk graph.
     */
    public int getLastGraphUpdateFrame() {
        return this.rebuildFrame;
    }

    /**
     * Returns the chunk section position which this render refers to in the world.
     */
    public ChunkSectionPos getChunkPos() {
        return ChunkSectionPos.from(this.chunkX, this.chunkY, this.chunkZ);
    }

    /**
     * Returns the chunk render column which this section render is a part of.
     */
    public ColumnRender<T> getColumn() {
        return this.column;
    }

    /**
     * Tests if the given chunk render is visible within the provided frustum.
     * @param frustum The frustum to test against
     * @return True if visible, otherwise false
     */
    public boolean isVisible(FrustumExtended frustum) {
        return frustum.fastAabbTest(this.boundsMinX, this.boundsMinY, this.boundsMinZ, this.boundsMaxX, this.boundsMaxY, this.boundsMaxZ);
    }

    /**
     * Ensures that all resources attached to the given chunk render are "ticked" forward. This should be called every
     * time before this render is drawn if {@link ChunkRenderContainer#canTick()} is true.
     */
    public void tick() {
        List<Sprite> sprites = this.getData().getAnimatedSprites();

        // This check is unnecessary if callers only perform ticking when we signal to do so, but just in case
        if (!sprites.isEmpty()) {// We would like to avoid allocating an iterator here
            // noinspection ForLoopReplaceableByForEach
            for (int i = 0, size = sprites.size(); i < size; i++) {
                SpriteUtil.ensureSpriteReady(sprites.get(i));
            }
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
    public double getSquaredDistance(BlockPos pos) {
        return this.getSquaredDistance(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    public double getSquaredDistanceXZ(Vector3d pos) {
        return this.getSquaredDistanceXZ(pos.x, pos.z);
    }

    /**
     * @return The squared distance from the center of this chunk in the world to the given position
     */
    public double getSquaredDistance(double x, double y, double z) {
        double xDist = x - this.getCenterX();
        double yDist = y - this.getCenterY();
        double zDist = z - this.getCenterZ();

        return (xDist * xDist) + (yDist * yDist) + (zDist * zDist);
    }

    /**
     * @return The squared distance from the center of this chunk in the world to the given position
     */
    public double getSquaredDistanceXZ(double x, double z) {
        double xDist = x - this.getCenterX();
        double zDist = z - this.getCenterZ();

        return (xDist * xDist) + (zDist * zDist);
    }

    /**
     * @return The x-coordinate of the center position of this chunk render
     */
    private double getCenterX() {
        return this.getOriginX() + 8.0D;
    }

    /**
     * @return The y-coordinate of the center position of this chunk render
     */
    private double getCenterY() {
        return this.getOriginY() + 8.0D;
    }

    /**
     * @return The z-coordinate of the center position of this chunk render
     */
    private double getCenterZ() {
        return this.getOriginZ() + 8.0D;
    }

    public byte getCullingState() {
        return this.cullingState;
    }

    /**
     * Returns the direction in which this node was traversed through in the chunk graph.
     */
    public Direction getDirection() {
        if (this.direction < 0) {
            return null;
        }

        return DirectionUtil.ALL_DIRECTIONS[this.direction];
    }

    /**
     * @return The frame index which this render was last visible
     */
    public int getLastVisibleFrame() {
        return this.lastVisibleFrame;
    }

    public boolean hasData() {
        return this.data != ChunkRenderData.EMPTY;
    }

    /**
     * @return True if the render can be ticked, otherwise false
     */
    public boolean canTick() {
        return !this.getData().getAnimatedSprites().isEmpty();
    }

    public BlockPos getOrigin() {
        return new BlockPos(this.getOriginX(), this.getOriginY(), this.getOriginZ());
    }

    public T getGraphicsState() {
        return this.graphicsState;
    }

    public void setGraphicsState(T state) {
        this.graphicsState = state;
    }
}
