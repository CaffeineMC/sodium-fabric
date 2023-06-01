package me.jellysquid.mods.sodium.client.render.chunk.graph;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.util.frustum.Frustum;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.util.math.Direction;

public class ChunkGraphInfo {
    private static final long DEFAULT_VISIBILITY_DATA = calculateVisibilityData(ChunkRenderData.EMPTY.getOcclusionData());

    private final RenderSection parent;

    private int lastVisibleFrame = -1;

    private long visibilityData;
    private short cullingState;

    public ChunkGraphInfo(RenderSection parent) {
        this.parent = parent;
        this.visibilityData = DEFAULT_VISIBILITY_DATA;
    }

    public void setLastVisibleFrame(int frame) {
        this.lastVisibleFrame = frame;
    }

    public int getLastVisibleFrame() {
        return this.lastVisibleFrame;
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

    public short computeQueuePop() {
        short retVal = (short) (cullingState & (((cullingState >> 8) & 0xFF) | 0xFF00));
        cullingState = 0;
        return retVal;
    }

    //The way this works now is
    public void updateCullingState(Direction flow) {
        int inbound = flow.ordinal();
        this.cullingState |= (visibilityData >> (6 * inbound)) & 0x3F;
        this.cullingState &= ~(1 << (inbound + 8));
    }

    public void setCullingState(short parent) {
        this.cullingState = (short) (parent & 0xFF00);
    }

    public void resetCullingState() {
        this.cullingState = -1;
    }

    public boolean isCulledByFrustum(Frustum frustum) {
        float x = this.getOriginX();
        float y = this.getOriginY();
        float z = this.getOriginZ();

        return !frustum.isBoxVisible(x, y, z, x + 16.0f, y + 16.0f, z + 16.0f);
    }

    /**
     * @return The x-coordinate of the origin position of this chunk render
     */
    public int getOriginX() {
        return this.parent.getChunkX() << 4;
    }

    /**
     * @return The y-coordinate of the origin position of this chunk render
     */
    public int getOriginY() {
        return this.parent.getChunkY() << 4;
    }

    /**
     * @return The z-coordinate of the origin position of this chunk render
     */
    public int getOriginZ() {
        return this.parent.getChunkZ() << 4;
    }
}
