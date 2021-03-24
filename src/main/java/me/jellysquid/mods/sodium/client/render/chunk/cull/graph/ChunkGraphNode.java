package me.jellysquid.mods.sodium.client.render.chunk.cull.graph;

import me.jellysquid.mods.sodium.client.render.chunk.cull.DirectionInt;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.util.math.FrustumExtended;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import me.jellysquid.mods.sodium.common.util.collections.TrackedArrayItem;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;

public class ChunkGraphNode implements TrackedArrayItem {
    private static final long DEFAULT_VISIBILITY_DATA = calculateVisibilityData(ChunkRenderData.EMPTY.getOcclusionData());

    private ChunkGraphNode adjacentUp;
    private ChunkGraphNode adjacentDown;
    private ChunkGraphNode adjacentNorth;
    private ChunkGraphNode adjacentSouth;
    private ChunkGraphNode adjacentWest;
    private ChunkGraphNode adjacentEast;

    private final int id;
    private final int chunkX, chunkY, chunkZ;

    private int lastVisibleFrame;

    private long visibilityData;
    private boolean empty;

    public ChunkGraphNode(int chunkX, int chunkY, int chunkZ, int id) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;

        this.id = id;

        this.visibilityData = DEFAULT_VISIBILITY_DATA;
        this.lastVisibleFrame = -1;

        this.empty = false;
    }

    public void setLastVisibleFrame(int frame) {
        this.lastVisibleFrame = frame;
    }

    public int getLastVisibleFrame() {
        return this.lastVisibleFrame;
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

    public void updateRenderData(ChunkRenderData data) {
        this.visibilityData = calculateVisibilityData(data.getOcclusionData());
        this.empty = data != ChunkRenderData.ABSENT && data.isEmpty();
    }

    private static long calculateVisibilityData(ChunkOcclusionData occlusionData) {
        long visibilityData = 0;

        for (Direction from : DirectionUtil.ALL_DIRECTIONS) {
            for (Direction to : DirectionUtil.ALL_DIRECTIONS) {
                if (occlusionData == null || occlusionData.isVisibleThrough(from, to)) {
                    visibilityData |= 1L << ((from.ordinal() * DirectionInt.COUNT) + to.ordinal());
                }
            }
        }

        return visibilityData;
    }

    @Override
    public int getId() {
        return this.id;
    }

    public boolean isCulledByFrustum(FrustumExtended frustum) {
        float x = this.getOriginX();
        float y = this.getOriginY();
        float z = this.getOriginZ();

        return !frustum.fastAabbTest(x, y, z, x + 16.0f, y + 16.0f, z + 16.0f);
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

    /**
     * @return The squared distance from the center of this chunk in the world to the given position
     */
    public double getSquaredDistance(double x, double y, double z) {
        double xDist = x - (this.getOriginX() + 8.0D);
        double yDist = y - (this.getOriginY() + 8.0D);
        double zDist = z - (this.getOriginZ() + 8.0D);

        return (xDist * xDist) + (yDist * yDist) + (zDist * zDist);
    }

    public long getVisibilityData() {
        return this.visibilityData;
    }

    public long getPosition() {
        return ChunkSectionPos.asLong(this.chunkX, this.chunkY, this.chunkZ);
    }

    public boolean isEmpty() {
        return this.empty;
    }

    public ChunkGraphNode getConnectedNode(int dir) {
        switch (dir) {
            case DirectionInt.DOWN:
                return this.adjacentDown;
            case DirectionInt.UP:
                return this.adjacentUp;
            case DirectionInt.NORTH:
                return this.adjacentNorth;
            case DirectionInt.SOUTH:
                return this.adjacentSouth;
            case DirectionInt.WEST:
                return this.adjacentWest;
            case DirectionInt.EAST:
                return this.adjacentEast;
            default:
                return null;
        }
    }

    public void setAdjacentNode(int dir, ChunkGraphNode node) {
        switch (dir) {
            case DirectionInt.DOWN:
                this.adjacentDown = node;
                break;
            case DirectionInt.UP:
                this.adjacentUp = node;
                break;
            case DirectionInt.NORTH:
                this.adjacentNorth = node;
                break;
            case DirectionInt.SOUTH:
                this.adjacentSouth = node;
                break;
            case DirectionInt.WEST:
                this.adjacentWest = node;
                break;
            case DirectionInt.EAST:
                this.adjacentEast = node;
                break;
        }
    }

}
