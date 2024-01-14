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

    private static final int BIT_COUNT = 32;
    private static final int FLIP_SIGN_MASK = 1 << (BIT_COUNT - 1);

    /**
     * Converts a float to a comparable integer value. This is used to compare
     * floating point values by their int bits (for example packed in a long).
     * 
     * The resulting integer can be treated as if it's unsigned and numbers the
     * floats from the smallest negative to the largest positive value.
     */
    public static int floatToComparableInt(float f) {
        // // uses Float.compare to avoid issues comparing -0.0f and 0.0f
        // return Float.floatToRawIntBits(f) ^ (Float.compare(f, 0f) > 0 ? 0x80000000 : 0xffffffff);

        var bits = Float.floatToRawIntBits(f);
        return bits ^ ((bits >> (BIT_COUNT - 1)) | FLIP_SIGN_MASK);
    }
}
