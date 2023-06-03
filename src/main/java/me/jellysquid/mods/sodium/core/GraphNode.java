package me.jellysquid.mods.sodium.core;

public class GraphNode {
    public final long connections;
    public final int flags;

    public GraphNode(long connections, int flags) {
        this.connections = connections;
        this.flags = flags;
    }
}
