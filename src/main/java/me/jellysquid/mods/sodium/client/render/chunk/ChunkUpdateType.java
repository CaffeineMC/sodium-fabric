package me.jellysquid.mods.sodium.client.render.chunk;

public enum ChunkUpdateType {
    INITIAL_BUILD,
    REBUILD,
    IMPORTANT_REBUILD;

    public static boolean canPromote(ChunkUpdateType prev, ChunkUpdateType next) {
        return prev == null || (prev == REBUILD && next == IMPORTANT_REBUILD);
    }
}
