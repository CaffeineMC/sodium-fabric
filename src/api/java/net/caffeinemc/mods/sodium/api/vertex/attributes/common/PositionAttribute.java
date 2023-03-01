package net.caffeinemc.mods.sodium.api.vertex.attributes.common;

import org.lwjgl.system.MemoryUtil;

public class PositionAttribute {
    public static void put(long ptr, float x, float y, float z) {
        MemoryUtil.memPutFloat(ptr + 0, x);
        MemoryUtil.memPutFloat(ptr + 4, y);
        MemoryUtil.memPutFloat(ptr + 8, z);
    }

    public static float getX(long ptr) {
        return MemoryUtil.memGetFloat(ptr + 0);
    }

    public static float getY(long ptr) {
        return MemoryUtil.memGetFloat(ptr + 4);
    }

    public static float getZ(long ptr) {
        return MemoryUtil.memGetFloat(ptr + 8);
    }
}
