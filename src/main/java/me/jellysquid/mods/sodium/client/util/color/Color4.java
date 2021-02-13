package me.jellysquid.mods.sodium.client.util.color;

public class Color4 {
    public final int r, g, b, a;

    public Color4(int r, int g, int b, int a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public static Color4 fromRGBA(int color) {
        return new Color4((color >> 16 & 0xFF), (color >> 8 & 0xFF), (color & 0xFF), (color >> 24 & 0xFF));
    }
}
