package me.jellysquid.mods.sodium.client.render.chunk.graph;

import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.util.math.Direction;

public class VisibilityEncoding {
    public static long encode(ChunkOcclusionData occlusionData) {
        long visibilityData = 0;

        for (Direction from : DirectionUtil.ALL_DIRECTIONS) {
            for (Direction to : DirectionUtil.ALL_DIRECTIONS) {
                if (from == to) {
                    continue;
                }

                if (occlusionData == null || occlusionData.isVisibleThrough(from, to)) {
                    visibilityData |= 1L << bit(from.ordinal(), to.ordinal());
                }
            }
        }

        return visibilityData;
    }

    private static int bit(int from, int to) {
        return (from * 8) + to;
    }

    // Returns a merged bit-field of the outgoing directions for each incoming direction
    public static int getOutgoingDirections(long data, int incoming) {
        long outgoing = 0L;

        for (int i = 0; i < 6; i++){
            outgoing |= ((incoming & (1 << i)) != 0 ? data : 0);
            data >>= 8;
        }

        return (int) (outgoing & 0b111111);
    }
}