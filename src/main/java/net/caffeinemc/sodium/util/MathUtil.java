package net.caffeinemc.sodium.util;

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
     * Returns {@param position} aligned to the next multiple of {@param alignment}.
     * @param position The position in bytes
     * @param alignment The alignment in bytes (must be a power-of-two)
     * @return The aligned position, either equal to or greater than {@param position}
     */
    public static int align(int position, int alignment) {
        return ((position - 1) + alignment) & -alignment;
    }
}
