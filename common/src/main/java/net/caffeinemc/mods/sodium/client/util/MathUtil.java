package net.caffeinemc.mods.sodium.client.util;

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
     * <p>
     * The resulting integer can be treated as if it's unsigned and numbers the
     * floats from the smallest negative to the largest positive value.
     * <p>
     * Reference: <a href="https://stackoverflow.com/questions/23900328/are-floats-bit-patterns-ordered">StackOverflow Answer</a>
     */
    public static int floatToComparableInt(float f) {
        var bits = Float.floatToRawIntBits(f);
        return bits ^ ((bits >> 31) & 0x7FFFFFFF);
    }

    public static float comparableIntToFloat(int i) {
        return Float.intBitsToFloat(i ^ ((i >> 31) & 0x7FFFFFFF));
    }
}
