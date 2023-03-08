package me.jellysquid.mods.sodium.client.render.chunk.graph;

public class GraphNode {
    public static final int LOADED_BIT = 1 << 31;

    private static final int REGION_MASK = (1 << 10) - 1, REGION_OFFSET = 20;
    private static final int FLAGS_MASK = (1 << 4) - 1, FLAGS_OFFSET = 16;
    private static final int CONNECTION_MASK = (1 << 16) - 1, CONNECTION_OFFSET = 0;

    public static int unpackRegion(int node) {
        return (node >>> REGION_OFFSET) & REGION_MASK;
    }

    public static int unpackFlags(int node) {
        return (node >>> FLAGS_OFFSET) & FLAGS_MASK;
    }

    public static int unpackConnections(int node) {
        return (node >>> CONNECTION_OFFSET) & CONNECTION_MASK;
    }

    public static int pack(int region, int flags, int connections) {
        return ((region & REGION_MASK) << REGION_OFFSET) | ((flags & FLAGS_MASK) << FLAGS_OFFSET) | ((connections & CONNECTION_MASK) << CONNECTION_OFFSET);
    }

    public static boolean isLoaded(int node) {
        return node != 0;
    }
}
