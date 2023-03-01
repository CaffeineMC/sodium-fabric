package net.caffeinemc.mods.sodium.api.util;

/**
 * Provides some utilities for packing and unpacking color components from packed integer colors in ARGB format. This
 * packed format is used by most of Minecraft, but special care must be taken to pack it into ABGR format before passing
 * it to OpenGL attributes.
 *
 * | 32        | 24        | 16        | 8          |
 * | 0110 1100 | 0110 1100 | 0110 1100 | 0110 1100  |
 * | Alpha     | Red       | Green     | Blue       |
 */
public class ColorARGB implements ColorU8 {
    private static final int ALPHA_COMPONENT_OFFSET = 24;
    private static final int RED_COMPONENT_OFFSET = 16;
    private static final int GREEN_COMPONENT_OFFSET = 8;
    private static final int BLUE_COMPONENT_OFFSET = 0;

    /**
     * Packs the specified color components into big-endian format for consumption by OpenGL.
     * @param r The red component of the color
     * @param g The green component of the color
     * @param b The blue component of the color
     * @param a The alpha component of the color
     */
    public static int pack(int r, int g, int b, int a) {
        return (a & COMPONENT_MASK) << ALPHA_COMPONENT_OFFSET |
                (r & COMPONENT_MASK) << RED_COMPONENT_OFFSET |
                (g & COMPONENT_MASK) << GREEN_COMPONENT_OFFSET |
                (b & COMPONENT_MASK) << BLUE_COMPONENT_OFFSET;
    }

    /**
     * @param color The packed 32-bit ARGB color to unpack
     * @return The red color component in the range of 0..255
     */
    public static int unpackAlpha(int color) {
        return color >> ALPHA_COMPONENT_OFFSET & COMPONENT_MASK;
    }

    /**
     * @param color The packed 32-bit ARGB color to unpack
     * @return The red color component in the range of 0..255
     */
    public static int unpackRed(int color) {
        return color >> RED_COMPONENT_OFFSET & COMPONENT_MASK;
    }

    /**
     * @param color The packed 32-bit ARGB color to unpack
     * @return The green color component in the range of 0..255
     */
    public static int unpackGreen(int color) {
        return color >> GREEN_COMPONENT_OFFSET & COMPONENT_MASK;
    }

    /**
     * @param color The packed 32-bit ARGB color to unpack
     * @return The blue color component in the range of 0..255
     */
    public static int unpackBlue(int color) {
        return color >> BLUE_COMPONENT_OFFSET & COMPONENT_MASK;
    }

    /**
     * Re-packs the ARGB color into a aBGR color with the specified alpha component.
     */
    public static int toABGR(int color, int alpha) {
        return Integer.reverseBytes(color << 8 | alpha);
    }

    public static int toABGR(int color) {
        return Integer.reverseBytes(color << 8);
    }
}
