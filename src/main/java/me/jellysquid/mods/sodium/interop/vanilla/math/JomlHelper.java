package me.jellysquid.mods.sodium.interop.vanilla.math;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

public class JomlHelper {
    public static void set(Matrix4f dst, com.mojang.math.Matrix4f src) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.callocFloat(16);
            src.store(buffer);

            dst.set(buffer);
        }
    }

    public static Matrix4f copy(com.mojang.math.Matrix4f src) {
        Matrix4f dst = new Matrix4f();
        set(dst, src);

        return dst;
    }

    public static void set(Matrix3f dst, com.mojang.math.Matrix3f src) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.callocFloat(12);
            src.store(buffer);

            dst.set(buffer);
        }
    }

    public static Matrix3f copy(com.mojang.math.Matrix3f src) {
        Matrix3f dst = new Matrix3f();
        set(dst, src);

        return dst;
    }
}
