package me.jellysquid.mods.sodium.client.render.chunk.cull.graph;

import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.util.math.FrustumExtended;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class ChunkGraphNode {
    private static final long DEFAULT_VISIBILITY_DATA = calculateVisibilityData(ChunkRenderData.EMPTY.getOcclusionData());

    private final ChunkGraphNode[] nodes = new ChunkGraphNode[DirectionUtil.ALL_DIRECTIONS.length];

    private final int id;
    private final int chunkX, chunkY, chunkZ;

    private int lastVisibleFrame = -1;

    private long visibilityData;
    private byte cullingState;

    public ChunkGraphNode(int chunkX, int chunkY, int chunkZ, int id) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;
        this.id = id;

        this.visibilityData = DEFAULT_VISIBILITY_DATA;
    }

    public ChunkGraphNode getConnectedNode(Direction dir) {
        return this.nodes[dir.ordinal()];
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

    public void setAdjacentNode(Direction dir, ChunkGraphNode node) {
        this.nodes[dir.ordinal()] = node;
    }

    public void setOcclusionData(ChunkOcclusionData occlusionData) {
        this.visibilityData = calculateVisibilityData(occlusionData);
    }

    private static long calculateVisibilityData(ChunkOcclusionData occlusionData) {
        long visibilityData = 0;

        for (Direction from : DirectionUtil.ALL_DIRECTIONS) {
            for (Direction to : DirectionUtil.ALL_DIRECTIONS) {
                if (occlusionData == null || occlusionData.isVisibleThrough(from, to)) {
                    visibilityData |= (1L << ((from.ordinal() << 3) + to.ordinal()));
                }
            }
        }

        return visibilityData;
    }

    public boolean isVisibleThrough(Direction from, Direction to) {
        return ((this.visibilityData & (1L << ((from.ordinal() << 3) + to.ordinal()))) != 0L);
    }

    public void setCullingState(byte parent, Direction dir) {
        this.cullingState = (byte) (parent | (1 << dir.ordinal()));
    }

    public boolean canCull(Direction dir) {
        return (this.cullingState & 1 << dir.ordinal()) != 0;
    }

    public byte getCullingState() {
        return this.cullingState;
    }

    public void resetCullingState() {
        this.cullingState = 0;
    }

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

    /**
     * @return The squared distance from the center of this chunk in the world to the given position
     */
    public double getSquaredDistance(double x, double y, double z) {
        double xDist = x - this.getCenterX();
        double yDist = y - this.getCenterY();
        double zDist = z - this.getCenterZ();

        return (xDist * xDist) + (yDist * yDist) + (zDist * zDist);
    }
}
