package me.jellysquid.mods.sodium.client.util.math;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

public class JomlHelper {
    public static void set(Matrix4f dst, net.minecraft.util.math.Matrix4f src) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.callocFloat(16);
            src.writeColumnMajor(buffer);

            dst.set(buffer);
        }
    }

    public static Matrix4f copy(net.minecraft.util.math.Matrix4f src) {
        Matrix4f dst = new Matrix4f();
        set(dst, src);

        return dst;
    }
}
