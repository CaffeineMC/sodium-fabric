package me.jellysquid.mods.sodium.client.util.color;

public class ColorMixer {
    private static final long MASK1 = 0x00FF00FF;
    private static final long MASK2 = 0xFF00FF00;

    /**
     * Mixes two ARGB colors using the given ratios. Use {@link ColorMixer#getStartRatio(float)} and
     * {@link ColorMixer#getEndRatio(float)} to convert a floating-point ratio into a integer ratio.
     *
     * This method takes 64-bit inputs to avoid overflows when mixing the alpha channel. The helper method
     * {@link ColorMixer#mix(int, int, int, int)} can be used with 32-bit inputs.
     *
     * @param c1 The first (starting) color to blend with in ARGB format
     * @param c2 The second (ending) color to blend with in ARGB format
     * @param f1 The ratio of the color {@param c1} as calculated by {@link ColorMixer#getStartRatio(float)}
     * @param f2 The ratio of the color {@param c2} as calculated by {@link ColorMixer#getEndRatio(float)}
     * @return The result of ((c1 * f1) + (c2 * f2) as an ARGB-encoded color
     */
    public static long mix(long c1, long c2, int f1, int f2) {
        return ((((((c1 & MASK1) * f1) + ((c2 & MASK1) * f2)) >> 8) & MASK1) |
                        (((((c1 & MASK2) * f1) + ((c2 & MASK2) * f2)) >> 8) & MASK2));
    }

    /**
     * Helper method to convert 32-bit integers to 64-bit integers and back.
     * @see ColorMixer#mix(long, long, int, int)
     */
    public static int mix(int c1, int c2, int f1, int f2) {
        return (int) mix(Integer.toUnsignedLong(c1), Integer.toUnsignedLong(c2), f1, f2);
    }

    public static int getStartRatio(float frac) {
        return (int) (256 * frac);
    }

    public static int getEndRatio(float frac) {
        return 256 - getStartRatio(frac);
    }


    public static int mulARGB(int a, int b) {
        float cr = ColorARGB.unpackRed(a) * ColorARGB.unpackRed(b);
        float cg = ColorARGB.unpackGreen(a) * ColorARGB.unpackGreen(b);
        float cb = ColorARGB.unpackBlue(a) * ColorARGB.unpackBlue(b);
        float ca = ColorARGB.unpackAlpha(a) * ColorARGB.unpackAlpha(b);

        return ColorARGB.pack((int) ColorU8.normalize(cr),
                (int) ColorU8.normalize(cg),
                (int) ColorU8.normalize(cb),
                (int) ColorU8.normalize(ca));
    }
}
