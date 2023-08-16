package me.jellysquid.mods.sodium.client.render.chunk;

public enum ChunkUpdateType {
    TRANSLUCENT_SORT(Integer.MAX_VALUE), // TODO: translucent sort queuing
    INITIAL_BUILD(128),
    REBUILD(Integer.MAX_VALUE),
    IMPORTANT_REBUILD(Integer.MAX_VALUE);

    private final int maximumQueueSize;

    ChunkUpdateType(int maximumQueueSize) {
        this.maximumQueueSize = maximumQueueSize;
    }

    public static boolean canPromote(ChunkUpdateType prev, ChunkUpdateType next) {
        return prev == null || prev == TRANSLUCENT_SORT || (prev == REBUILD && next == IMPORTANT_REBUILD);
    }

    public int getMaximumQueueSize() {
        return this.maximumQueueSize;
    }

    public boolean isImportant() {
        return this == IMPORTANT_REBUILD;
    }
}
