package me.jellysquid.mods.sodium.client.util;

public class MathUtil {
    // TODO: Use Math#fma in Java 9+
    public static float fma(float a, float b, float c) {
        return a * b + c;
    }
}
