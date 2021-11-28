package me.jellysquid.mods.sodium.client.util.color;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorOperators;

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
        return IntVector.fromArray(
                IntVector.SPECIES_128,
                new int[]{a, b, g, r},
                0
        ).lanewise(VectorOperators.AND, 0xFF)
                .lanewise(
                        VectorOperators.LSHL,
                        IntVector.fromArray(
                                IntVector.SPECIES_128,
                                new int[]{24, 16, 8, 0},
                                0
                        )
                ).reduceLanes(VectorOperators.OR);
//        return (a & 0xFF) << 24 | (b & 0xFF) << 16 | (g & 0xFF) << 8 | (r & 0xFF);
    }

    /**
     * @see ColorABGR#pack(int, int, int, int)
     */
    public static int pack(float r, float g, float b, float a) {
        float[] c = FloatVector.fromArray(
            FloatVector.SPECIES_128,
            new float[]{r, g, b, a},
            0
        ).mul(COMPONENT_RANGE).toArray();
        return pack((int) c[0], (int) c[1], (int) c[2], (int) c[3]);
    }

    /**
     * Multiplies the RGB components of the packed ABGR color using the given scale factors.
     * @param color The ABGR packed color to be multiplied
     * @param rw The red component scale factor
     * @param gw The green component scale factor
     * @param bw The blue component scale factor
     */
    public static int mul(int color, float rw, float gw, float bw) {
        float r = unpackRed(color) * rw;
        float g = unpackGreen(color) * gw;
        float b = unpackBlue(color) * bw;

        return pack((int) r, (int) g, (int) b, 0xFF);
    }

    public static int mul(int color, float w) {
        return mul(color, w, w, w);
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
