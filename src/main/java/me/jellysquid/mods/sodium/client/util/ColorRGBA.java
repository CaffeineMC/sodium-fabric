package me.jellysquid.mods.sodium.client.util;

/**
 * Provides some utilities for packing and unpacking color components from packed integer colors in ARGB format. This
 * packed format is used by most of Minecraft, but special care must be taken to pack it into ABGR format (big-endian)
 * before passing it to OpenGL attributes.
 *
 * | 32        | 24        | 16        | 8          |
 * | 0110 1100 | 0110 1100 | 0110 1100 | 0110 1100  |
 * | Alpha     | Blue      | Green     | Red        |
 */
public class ColorRGBA {
    /**
     * The maximum value of a color component.
     */
    private static final float COMPONENT_RANGE = 255.0f;

    /**
     * Constant value which can be multiplied with a floating-point color component to get the normalized value. The
     * multiplication is slightly faster than a floating point division, and this code is a hot path which justifies it.
     */
    private static final float NORM_RGB = 1.0f / COMPONENT_RANGE;

    /**
     * Packs the specified color components into big-endian format for consumption by OpenGL.
     * @param r The red component of the color
     * @param g The green component of the color
     * @param b The blue component of the color
     * @param a The alpha component of the color
     */
    public static int packABGR(int r, int g, int b, int a) {
        return (a & 0xFF) << 24 | (b & 0xFF) << 16 | (g & 0xFF) << 8 | (r & 0xFF);
    }

    /**
     * @see ColorRGBA#packABGR(int, int, int, int)
     */
    public static int packABGR(float r, float g, float b, float a) {
        return packABGR((int) (r * COMPONENT_RANGE), (int) (g * COMPONENT_RANGE), (int) (b * COMPONENT_RANGE), (int) (a * COMPONENT_RANGE));
    }

    /**
     * Re-packs the RGBA color into a ABGR color with the specified alpha component.
     */
    public static int repackABGR(int color, int alpha) {
        return ColorRGBA.packABGR(ColorRGBA.unpackRed(color), ColorRGBA.unpackGreen(color), ColorRGBA.unpackBlue(color), alpha);
    }

    /**
     * Multiplies the RGB channels of the packed color using the given scale factors.
     * @param color The RGBA packed color to be multiplied
     * @param rw The red component scale factor
     * @param gw The green component scale factor
     * @param bw The blue component scale factor
     */
    public static int mulPackABGR(int color, float rw, float gw, float bw) {
        float r = unpackRed(color) * rw;
        float g = unpackGreen(color) * gw;
        float b = unpackBlue(color) * bw;

        return packABGR((int) r, (int) g, (int) b, 0xFF);
    }

    /**
     * @param color The packed 32-bit ARGB color to unpack
     * @return The red color component in the range of 0..255
     */
    public static int unpackAlpha(int color) {
        return color >> 24 & 0xFF;
    }

    /**
     * @param color The packed 32-bit ARGB color to unpack
     * @return The red color component in the range of 0..255
     */
    public static int unpackRed(int color) {
        return color >> 16 & 0xFF;
    }

    /**
     * @param color The packed 32-bit ARGB color to unpack
     * @return The green color component in the range of 0..255
     */
    public static int unpackGreen(int color) {
        return color >> 8 & 0xFF;
    }

    /**
     * @param color The packed 32-bit ARGB color to unpack
     * @return The blue color component in the range of 0..255
     */
    public static int unpackBlue(int color) {
        return color & 0xFF;
    }

    /**
     * Normalizes a color component to the range of 0..1.
     */
    public static float normalize(float v) {
        return v * NORM_RGB;
    }
}
