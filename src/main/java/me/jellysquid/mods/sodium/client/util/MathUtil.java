package me.jellysquid.mods.sodium.client.util;

public class MathUtil {
    public static boolean isPowerOfTwo(int n) {
        return ((n & (n - 1)) == 0);
    }
}
