package net.caffeinemc.sodium.render.chunk.state;

public class ChunkRenderFlag {
    public static int HAS_BLOCK_ENTITIES = 1 << 0;
    public static int HAS_GLOBAL_BLOCK_ENTITIES = 1 << 1;
    public static int HAS_MESHES = 1 << 2;
    public static int HAS_TICKING_TEXTURES = 1 << 3;

    public static boolean has(int word, int flag) {
        return (word & flag) != 0;
    }
}
