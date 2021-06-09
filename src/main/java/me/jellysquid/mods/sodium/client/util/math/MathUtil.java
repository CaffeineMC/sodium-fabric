package me.jellysquid.mods.sodium.client.util.math;

public class MathUtil {
    public static int nextInterval(int num, int interval) {
        return interval * ((num + interval - 1) / interval);
    }
}
