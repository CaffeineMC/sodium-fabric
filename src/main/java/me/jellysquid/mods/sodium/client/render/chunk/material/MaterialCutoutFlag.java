package me.jellysquid.mods.sodium.client.render.chunk.material;

public class MaterialCutoutFlag {
    public static final int NONE = 0;
    public static final int HALF = 1;
    public static final int TENTH = 2;
    public static final int ZERO = 3;

    public static int shift(int flag) {
        return flag << 1;
    }
}
