package net.caffeinemc.sodium.render.chunk.state;

public class SectionRenderFlags {
    public static byte HAS_BLOCK_ENTITIES = 1 << 0;
    public static byte HAS_GLOBAL_BLOCK_ENTITIES = 1 << 1;
    public static byte HAS_TICKING_TEXTURES = 1 << 2;
    public static byte HAS_TERRAIN_MODELS = 1 << 3;
    public static byte NEEDS_UPDATE = 1 << 4;
    public static byte NEEDS_UPDATE_REBUILD = 1 << 5;
    public static byte NEEDS_UPDATE_IMPORTANT = 1 << 6;

    public static boolean has(byte word, byte flags) {
        return (word & flags) != 0;
    }
    
    public static void set(byte[] array, int idx, byte flags) {
        array[idx] |= flags;
    }
    
    public static void unset(byte[] array, int idx, byte flags) {
        array[idx] &= ~flags;
    }
}
