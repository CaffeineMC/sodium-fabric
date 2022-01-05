package me.jellysquid.mods.sodium.opengl.util;

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class MemCmp {
    public static boolean compare(FloatBuffer a, FloatBuffer b) {
        if (a.capacity() != b.capacity()) {
            return false;
        }

        return compareInt(MemoryUtil.memAddress(a, 0), MemoryUtil.memAddress(b, 0), a.remaining() * 4);
    }

    public static boolean compare(IntBuffer a, IntBuffer b) {
        if (a.capacity() != b.capacity()) {
            return false;
        }

        return compareInt(MemoryUtil.memAddress(a, 0), MemoryUtil.memAddress(b, 0), a.remaining() * 4);
    }

    private static boolean compareInt(long a, long b, int bytes) {
        boolean ret = true;

        for (int i = 0; i < bytes; i += 4) {
            ret &= MemoryUtil.memGetInt(a + i) == MemoryUtil.memGetInt(b + i);
        }

        return ret;
    }
}
