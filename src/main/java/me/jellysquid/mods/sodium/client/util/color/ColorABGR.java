package me.jellysquid.mods.sodium.client.util.color;

/**
 * Provides some utilities for packing and unpacking color components from packed integer colors in ABGR format, which
 * is used by OpenGL for color vectors.
 *
 * | 32        | 24        | 16        | 8          |
 * | 0110 1100 | 0110 1100 | 0110 1100 | 0110 1100  |
 * | Alpha     | Blue      | Green     | Red        |
 */
public class ColorABGR implements ColorU8 {
    /**
     * Packs the specified color components into ABGR format.
     * @param r The red component of the color
     * @param g The green component of the color
     * @param b The blue component of the color
     * @param a The alpha component of the color
     */
    public static int pack(int r, int g, int b, int a) {
        return (a & 0xFF) << 24 | (b & 0xFF) << 16 | (g & 0xFF) << 8 | (r & 0xFF);
    }

    /**
     * Packs the specified color components into ABGR format.
     * @param rgb The red/green/blue component of the color
     * @param alpha The alpha component of the color
     */
    public static int withAlpha(int rgb, float alpha) {
        return withAlpha(rgb, (int) (alpha * COMPONENT_RANGE) & 0xFF);
    }

    /**
     * Packs the specified color components into ABGR format.
     * @param rgb The red/green/blue component of the color
     * @param alpha The alpha component of the color
     */
    public static int withAlpha(int rgb, int alpha) {
        return alpha << 24 | (rgb & 0xFFFFFF);
    }

    /**
     * @see ColorABGR#pack(int, int, int, int)
     */
    public static int pack(float r, float g, float b, float a) {
        return pack((int) (r * COMPONENT_RANGE), (int) (g * COMPONENT_RANGE), (int) (b * COMPONENT_RANGE), (int) (a * COMPONENT_RANGE));
    }

    /**
     * @param color The packed 32-bit ABGR color to unpack
     * @return The red color component in the range of 0..255
     */
    public static int unpackRed(int color) {
        return color & 0xFF;
    }

    /**
     * @param color The packed 32-bit ABGR color to unpack
     * @return The green color component in the range of 0..255
     */
    public static int unpackGreen(int color) {
        return color >> 8 & 0xFF;
    }

    /**
     * @param color The packed 32-bit ABGR color to unpack
     * @return The blue color component in the range of 0..255
     */
    public static int unpackBlue(int color) {
        return color >> 16 & 0xFF;
    }

    /**
     * @param color The packed 32-bit ABGR color to unpack
     * @return The red color component in the range of 0..255
     */
    public static int unpackAlpha(int color) {
        return color >> 24 & 0xFF;
    }

    public static int pack(float r, float g, float b) {
        return pack(r, g, b, 255);
    }
}
