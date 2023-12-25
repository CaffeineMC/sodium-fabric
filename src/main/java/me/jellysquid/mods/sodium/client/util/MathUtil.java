package me.jellysquid.mods.sodium.client.util;

public class MathUtil {
    /**
     * @return True if the specified number is greater than zero and is a power of two, otherwise false
     */
    public static boolean isPowerOfTwo(int n) {
        return ((n & (n - 1)) == 0);
    }

    public static long toMib(long bytes) {
        return bytes / (1024L * 1024L); // 1 MiB = 1048576 (2^20) bytes
    }

    /**
     * Converts a float to a comparable integer value. This is used to compare
     * floating point values by their int bits (for example packed in a long).
     */
    public static int floatToComparableInt(float f) {
        return Float.floatToRawIntBits(f) ^ (f >= 0 ? 0x80000000 : 0xffffffff);
    }
}
