package me.jellysquid.mods.sodium.client.render.chunk.graph;

public class GraphNode {
    private static final long CONNECTIONS_MASK = (1L << 48) - 1;
    private static final int CONNECTIONS_OFFSET = 0;
    private static final long FLAGS_MASK = (1L << 8) - 1;
    private static final int FLAGS_OFFSET = 48;

    public static long pack(long connections, int flags) {
        return ((connections & CONNECTIONS_MASK) << CONNECTIONS_OFFSET) |
                ((flags & FLAGS_MASK) << FLAGS_OFFSET);
    }

    public static long unpackConnections(long packed) {
        return (packed >> CONNECTIONS_OFFSET) & CONNECTIONS_MASK;
    }

    public static int unpackFlags(long packed) {
        return (int) ((packed >> FLAGS_OFFSET) & FLAGS_MASK);
    }

    public static boolean isLoaded(long node) {
        return node != 0L; // a loaded node will always have one bit set from the IS_LOADED flag
    }
}
