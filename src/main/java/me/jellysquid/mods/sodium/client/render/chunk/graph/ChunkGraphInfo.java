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


    //The way this works now is that the culling state contains 2 inner states
    // visited directions mask, and visitable direction mask
    //On graph start, the root node(s) have the visit and visitable masks set to all visible
    // when a chunk section is popped off the queue, the visited direction mask is anded with the
    // visitable direction mask to return a bitfield containing what directions the graph can flow too
    //When a chunk is visited in the graph the inbound direction is masked off from the visited direction mask
    // and the visitable direction mask is updated (ored) with the visibilityData of the inbound direction
    //When a chunk hasnt been visited before, it uses the parents data as the initial visited direction mask

    public short computeQueuePop() {
        short retVal = (short) (cullingState & (((cullingState >> 8) & 0xFF) | 0xFF00));
        cullingState = 0;
        return retVal;
    }

    public void updateCullingState(Direction flow, short parent) {
        int inbound = flow.ordinal();
        this.cullingState |= (visibilityData >> (inbound<<3)) & 0xFF;
        this.cullingState &= ~(1 << (inbound + 8));
        //NOTE: this isnt strictly needed, due to the properties provided from the bfs search (never backtracking),
        // but just incase/better readability/understandability
        this.cullingState &= parent|0x00FF;
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
