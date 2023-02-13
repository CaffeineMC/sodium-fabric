package net.caffeinemc.mods.sodium.api.vertex.attributes.common;

import org.joml.Vector2f;
import org.lwjgl.system.MemoryUtil;

public class TextureAttribute {
    public static void put(long ptr, Vector2f vec) {
        put(ptr, vec.x(), vec.y());
    }

    public static void put(long ptr, float u, float v) {
        MemoryUtil.memPutFloat(ptr + 0, u);
        MemoryUtil.memPutFloat(ptr + 4, v);
    }

    public static Vector2f get(long ptr) {
        return new Vector2f(getU(ptr), getV(ptr));
    }

    public static float getU(long ptr) {
        return MemoryUtil.memGetFloat(ptr + 0);
    }

    public static float getV(long ptr) {
        return MemoryUtil.memGetFloat(ptr + 4);
    }
}
