package me.jellysquid.mods.sodium.client.render.chunk.occlusion;


import net.minecraft.client.render.chunk.ChunkOcclusionData;
import org.jetbrains.annotations.NotNull;

public class VisibilityEncoding {
    public static final long NULL = 0L;

    public static long encode(@NotNull ChunkOcclusionData occlusionData) {
        long visibilityData = 0;

        for (int from = 0; from < GraphDirection.COUNT; from++) {
            for (int to = 0; to < GraphDirection.COUNT; to++) {
                if (occlusionData.isVisibleThrough(GraphDirection.toEnum(from), GraphDirection.toEnum(to))) {
                    visibilityData |= 1L << bit(from, to);
                }
            }
        }

        return visibilityData;
    }

    private static int bit(int from, int to) {
        return (from * 8) + to;
    }

    // Returns a merged bit-field of the outgoing directions for each incoming direction
    public static int getConnections(long visibilityData, int incomingDirections) {
        long outgoing = (((0b0000001_0000001_0000001_0000001_0000001_0000001L * Integer.toUnsignedLong(incomingDirections)) & 0x010101010101L) * 0xFF) // turn bitmask into lane wise mask
            & visibilityData; // apply visibility to incoming
        outgoing |= outgoing >> 32; // fold top 32 bits onto bottom 32 bits
        outgoing |= outgoing >> 16; // fold top 16 bits onto bottom 16 bits
        outgoing |= outgoing >> 8; // fold top 8 bits onto bottom 8 bits

        return (int) (outgoing & GraphDirection.ALL);
    }
}