package net.caffeinemc.mods.sodium.api.vertex.attributes.common;

import org.lwjgl.system.MemoryUtil;

public class LightAttribute {
    public static void set(long ptr, int light) {
        MemoryUtil.memPutInt(ptr + 0, light);
    }

    public static int get(long ptr) {
        return MemoryUtil.memGetInt(ptr);
    }
}
