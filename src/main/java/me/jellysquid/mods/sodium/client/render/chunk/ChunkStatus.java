package me.jellysquid.mods.sodium.client.render.chunk;

// TODO: convert to bitfield
public enum ChunkStatus {
    NOT_LOADED,
    AWAITING_LIGHT,
    READY;

    public static final ChunkStatus[] VALUES = ChunkStatus.values();
}
