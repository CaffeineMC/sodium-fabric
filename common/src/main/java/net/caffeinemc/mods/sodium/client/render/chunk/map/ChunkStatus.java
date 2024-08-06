package net.caffeinemc.mods.sodium.client.render.chunk.map;

public class ChunkStatus {
    public static final int FLAG_HAS_BLOCK_DATA = 1;
    public static final int FLAG_HAS_LIGHT_DATA = 2;
    public static final int FLAG_ALL = FLAG_HAS_BLOCK_DATA | FLAG_HAS_LIGHT_DATA;
}