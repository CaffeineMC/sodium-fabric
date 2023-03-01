package net.caffeinemc.mods.sodium.api.util;

public class ColorMixer {
    private static final int CHANNEL_MASK = 0x00FF00FF;

    /**
     * Mixes a 32-bit color (with packed 8-bit components) into another using the given ratio. This is equivalent to
     * (color1 * ratio) + (color2 * (1.0 - ratio)) but uses bitwise trickery for maximum performance.
     * <p>
     * The order of the channels within the packed color does not matter, and the output color will always
     * have the same ordering as the input colors.
     *
     * @param aColor The color to mix towards
     * @param bColor The color to mix away from
     * @param ratio The percentage (in 0.0..1.0 range) to mix the first color into the second color
     * @return The mixed color in packed 32-bit format
     */
    public static int mix(int aColor, int bColor, float ratio) {
        int aRatio = (int) (256 * ratio); // int(ratio)
        int bRatio = 256 - aRatio; // int(1.0 - ratio)

        // Mask off and shift two components from each color into a packed vector of 16-bit components, where the
        // high 8 bits are all zeroes.
        int a1 = (aColor >> 0) & CHANNEL_MASK;
        int b1 = (bColor >> 0) & CHANNEL_MASK;
        int a2 = (aColor >> 8) & CHANNEL_MASK;
        int b2 = (bColor >> 8) & CHANNEL_MASK;

        // Multiply the packed 16-bit components against each mix factor, and add the components of each color
        // to produce the mixed result. This will never overflow since both 16-bit integers are in 0..255 range.

        // Then, shift the high 8 bits of each packed 16-bit component into the low 8 bits, and mask off the high bits of
        // each 16-bit component to produce a vector of packed 8-bit components, where every other component is empty.
        int c1 = (((a1 * aRatio) + (b1 * bRatio)) >> 8) & CHANNEL_MASK;
        int c2 = (((a2 * aRatio) + (b2 * bRatio)) >> 8) & CHANNEL_MASK;

        // Join the color components into the original order
        return ((c1 << 0) | (c2 << 8));
    }

    /**
     * Multiplies the 32-bit colors (with packed 8-bit components) together.
     * <p>
     * The order of the channels within the packed color does not matter, and the output color will always
     * have the same ordering as the input colors.
     *
     * @param a The first color to multiply
     * @param b The second color to multiply
     * @return The multiplied color in packed 32-bit format
     */
    public static int mul(int a, int b) {
        // Take each 8-bit component pair, multiply them together to create intermediate 16-bit integers,
        // and then shift the high half of each 16-bit integer into 8-bit integers.
        int c0 = (((a >>  0) & 0xFF) * ((b >>  0) & 0xFF)) >> 8;
        int c1 = (((a >>  8) & 0xFF) * ((b >>  8) & 0xFF)) >> 8;
        int c2 = (((a >> 16) & 0xFF) * ((b >> 16) & 0xFF)) >> 8;
        int c3 = (((a >> 24) & 0xFF) * ((b >> 24) & 0xFF)) >> 8;

        // Pack the components
        return (c0 <<  0) | (c1 <<  8) | (c2 << 16) | (c3 << 24);
    }
}
