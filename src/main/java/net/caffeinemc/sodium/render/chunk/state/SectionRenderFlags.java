package net.caffeinemc.sodium.render.chunk.state;

public class SectionRenderFlags {
    public static byte HAS_BLOCK_ENTITIES = 1 << 0;
    public static byte HAS_GLOBAL_BLOCK_ENTITIES = 1 << 1;
    public static byte HAS_TICKING_TEXTURES = 1 << 2;
    public static byte HAS_TERRAIN_MODELS = 1 << 3;
    public static byte NEEDS_UPDATE = 1 << 4;
    public static byte NEEDS_UPDATE_REBUILD = 1 << 5;
    public static byte NEEDS_UPDATE_IMPORTANT = 1 << 6;
    
    public static byte ALL_CONTENT_FLAGS = (byte) (HAS_BLOCK_ENTITIES | HAS_GLOBAL_BLOCK_ENTITIES | HAS_TICKING_TEXTURES | HAS_TERRAIN_MODELS);
    public static byte ALL_UPDATE_FLAGS = (byte) (NEEDS_UPDATE | NEEDS_UPDATE_REBUILD | NEEDS_UPDATE_IMPORTANT);

    public static boolean hasAny(byte word, int flags) {
        return (word & flags) != 0;
    }
    
    public static boolean hasAll(byte word, int flags) {
        return (word & flags) == flags;
    }
}
