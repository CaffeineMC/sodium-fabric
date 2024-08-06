package net.caffeinemc.mods.sodium.client.util;

public class BitwiseMath {
    // returns (1) if (a < b), otherwise (0)
    // valid for all values of (a) and (b)
    public static int lessThan(int a, int b) {
        return (a - b) >>> 31;
    }

    // returns (1) if (a > b), otherwise (0)
    // valid for all values of (a) and (b)
    public static int greaterThan(int a, int b) {
        return (b - a) >>> 31;
    }
}