package me.jellysquid.mods.sodium.client.util;

import org.apache.commons.lang3.Validate;

public class MathUtil {
    /**
     * @return True if the specified number is greater than zero and is a power of two, otherwise false
     */
    public static boolean isPowerOfTwo(int n) {
        return ((n & (n - 1)) == 0);
    }

    public static long toMib(long x) {
        return x / 1024L / 1024L;
    }

    /**
     * Returns {@param num} aligned to the next multiple of {@param alignment}.
     * @param num The number that will be rounded if needed
     * @param alignment The multiple that the output will be rounded to (must be a power-of-two)
     * @return The aligned position, either equal to or greater than {@param num}
     */
    public static int align(int num, int alignment) {
        Validate.isTrue(isPowerOfTwo(alignment), "alignment needs to be a power of two");
        int additive = alignment - 1;
        int mask = ~additive;
        return (num + additive) & mask;
    }
}
