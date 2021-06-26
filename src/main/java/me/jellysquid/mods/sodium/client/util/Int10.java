package me.jellysquid.mods.sodium.client.util;

public class Int10 {
    public static int pack(int x, int y, int z) {
        return (z << 20) | (y << 10) | x;
    }
}
