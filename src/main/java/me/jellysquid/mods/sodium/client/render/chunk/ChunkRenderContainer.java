package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.FrustumExtended;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;

import java.lang.reflect.Array;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The render state object for a chunk section. This contains all the graphics state for each render pass along with
 * data about the render in the chunk visibility graph.
 */
public class ChunkRenderContainer<T extends ChunkGraphicsState> {
    private final SodiumWorldRenderer worldRenderer;
    private final int chunkX, chunkY, chunkZ;

    private final T[] graphicsStates;

    private ChunkRenderData data = ChunkRenderData.ABSENT;
    private CompletableFuture<Void> rebuildTask = null;

    private boolean needsRebuild;
    private boolean needsImportantRebuild;

    private Direction direction;
    private int visibleFrame = -1;
    private byte cullingState;
    private byte visibleFaces;
    private long visibilityData;

    private final ChunkRenderContainer<T>[] adjacent;
    private boolean tickable;
    private boolean hasAnyGraphicsState;

    public ChunkRenderContainer(ChunkRenderBackend<T> backend, SodiumWorldRenderer worldRenderer, int chunkX, int chunkY, int chunkZ) {
        this.worldRenderer = worldRenderer;

        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;

        // noinspection unchecked
        this.adjacent = new ChunkRenderContainer[DirectionUtil.DIRECTION_COUNT];

        //noinspection unchecked
        this.graphicsStates = (T[]) Array.newInstance(backend.getGraphicsStateType(), BlockRenderPass.COUNT);
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
     * @return True if the adjacent chunk can be seen through this one, otherwise false
     */
    public boolean isVisibleThrough(Direction from, Direction to) {
        return (this.visibilityData & (1L << ((from.ordinal() << 3) + to.ordinal()))) != 0L;
    }

    /**
     * Deletes all data attached to this render and drops any pending tasks. This should be used when the render falls
     * out of view or otherwise needs to be destroyed. After the render has been destroyed, the object can no longer
     * be used.
     */
    public void delete() {
        this.cancelRebuildTask();
        this.setData(ChunkRenderData.ABSENT);
        this.deleteGraphicsState();
    }

    private void deleteGraphicsState() {
        T[] states = this.graphicsStates;

        for (int i = 0; i < states.length; i++) {
            T state = states[i];

            if (state != null) {
                state.delete();
                states[i] = null;
            }
        }
    }

    public void setData(ChunkRenderData info) {
        if (info == null) {
            throw new NullPointerException("Mesh information must not be null");
        }

        this.data = info;
        this.visibilityData = 0;

        for (Direction from : DirectionUtil.ALL_DIRECTIONS) {
            for (Direction to : DirectionUtil.ALL_DIRECTIONS) {
                if (this.data.isVisibleThrough(from, to)) {
                    this.visibilityData |= (1L << ((from.ordinal() << 3) + to.ordinal()));
                }
            }
        }

        this.worldRenderer.onChunkRenderUpdated(this.data, info);
        this.tickable = !info.getAnimatedSprites().isEmpty();
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
    public void setCullingState(byte parent, Direction dir) {
        this.cullingState = (byte) (parent | (1 << dir.ordinal()));
    }

    /**
     * @param dir The direction of the neighbor
     * @return True if this render can cull an adjacent neighbor, otherwise false
     */
    public boolean canCull(Direction dir) {
        return (this.cullingState & 1 << dir.ordinal()) != 0;
    }

    /**
     * Resets the state of this render in the chunk graph. This will be called on the seed node before the breadth-first
     * search is intitiated.
     */
    public void resetGraphState() {
        this.direction = null;
        this.cullingState = 0;
    }

    /**
     * Sets the direction in which this node was traversed through on the chunk graph.
     */
    public void setDirection(Direction dir) {
        this.direction = dir;
    }

    /**
     * Sets the frame index which this render was last updated on the chunk graph.
     */
    public void setVisibleFrame(int frame) {
        this.visibleFrame = frame;
    }

    /**
     * Returns the last frame index which this render was updated on the chunk graph.
     */
    public int getLastVisibleFrame() {
        return this.visibleFrame;
    }

    /**
     * Returns the chunk section position which this render refers to in the world.
     */
    public ChunkSectionPos getChunkPos() {
        return ChunkSectionPos.from(this.chunkX, this.chunkY, this.chunkZ);
    }

    /**
     * Tests if the given chunk render is visible within the provided frustum.
     * @param frustum The frustum to test against
     * @return True if visible, otherwise false
     */
    public boolean isOutsideFrustum(FrustumExtended frustum) {
        float x = this.getOriginX();
        float y = this.getOriginY();
        float z = this.getOriginZ();

        return !frustum.fastAabbTest(x, y, z, x + 16.0f, y + 16.0f, z + 16.0f);
    }

    /**
     * Ensures that all resources attached to the given chunk render are "ticked" forward. This should be called every
     * time before this render is drawn if {@link ChunkRenderContainer#isTickable()} is true.
     */
    public void tick() {
        List<Sprite> sprites = this.getData().getAnimatedSprites();

        // We would like to avoid allocating an iterator here
        // noinspection ForLoopReplaceableByForEach
        for (int i = 0, size = sprites.size(); i < size; i++) {
            SpriteUtil.markSpriteActive(sprites.get(i));
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

    public int getRenderX() {
        return this.getOriginX() - 8;
    }

    public int getRenderY() {
        return this.getOriginY() - 8;
    }

    public int getRenderZ() {
        return this.getOriginZ() - 8;
    }

    /**
     * @return The squared distance from the center of this chunk in the world to the center of the block position
     * given by {@param pos}
     */
    public double getSquaredDistance(BlockPos pos) {
        return this.getSquaredDistance(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
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
        return this.direction;
    }

    public BlockPos getRenderOrigin() {
        return new BlockPos(this.getRenderX(), this.getRenderY(), this.getRenderZ());
    }

    public T[] getGraphicsStates() {
        return this.graphicsStates;
    }

    public void setGraphicsState(BlockRenderPass pass, T state) {
        this.graphicsStates[pass.ordinal()] = state;
    }

    public boolean canRebuild() {
        for (Direction dir : DirectionUtil.HORIZONTAL_DIRECTIONS) {
            ChunkRenderContainer<T> adj = this.adjacent[dir.ordinal()];

            if (adj == null) {
                return false;
            }

            Direction corner;

            // Access the adjacent corner chunk from the neighbor in this direction
            if (dir == Direction.NORTH) {
                corner = Direction.EAST;
            } else if (dir == Direction.SOUTH) {
                corner = Direction.WEST;
            } else if (dir == Direction.WEST) {
                corner = Direction.NORTH;
            } else if (dir == Direction.EAST) {
                corner = Direction.SOUTH;
            } else {
                continue;
            }

            // If no neighbor has been attached, the chunk is not present
            if (adj.getAdjacentRender(corner) == null) {
                return false;
            }
        }

        return true;
    }

    /**
     * @return The squared distance from the center of this chunk in the world to the given position
     */
    public double getSquaredDistanceXZ(double x, double z) {
        double xDist = x - this.getCenterX();
        double zDist = z - this.getCenterZ();

        return (xDist * xDist) + (zDist * zDist);
    }

    public void setAdjacentRender(Direction dir, ChunkRenderContainer<T> adj) {
        this.adjacent[dir.ordinal()] = adj;
    }

    public ChunkRenderContainer<T> getAdjacentRender(Direction dir) {
        return this.adjacent[dir.ordinal()];
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

    public void resetVisibleFaces() {
        this.visibleFaces = (byte) (1 << ModelQuadFacing.NONE.ordinal());
    }

    public void markFaceVisible(ModelQuadFacing facing) {
        this.visibleFaces |= (byte) (1 << facing.ordinal());
    }

    public byte getVisibleFaces() {
        return this.visibleFaces;
    }

    public ChunkRenderBounds getBounds() {
        return this.data.getBounds();
    }

    public void markAllFacesVisible() {
        this.visibleFaces = 0b1111111;
    }

    public T getGraphicsState(BlockRenderPass pass) {
        return this.graphicsStates[pass.ordinal()];
    }

    public boolean isTickable() {
        return this.tickable;
    }

    public boolean hasAnyGraphicsState() {
        return this.hasAnyGraphicsState;
    }
}
