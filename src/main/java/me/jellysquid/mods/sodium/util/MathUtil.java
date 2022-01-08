package me.jellysquid.mods.sodium.util;

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

    public static long roundUpToMultiple(long value, long divisor) {
        return ceilDiv(value, divisor) * divisor;
    }

    public static long ceilDiv(long a, long b) {
        return -Math.floorDiv(-a, b);
    }

}
