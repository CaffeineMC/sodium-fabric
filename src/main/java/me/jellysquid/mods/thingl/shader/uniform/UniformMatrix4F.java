package me.jellysquid.mods.thingl.shader.uniform;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

public class UniformMatrix4F extends Uniform<Matrix4f> {
    public UniformMatrix4F(int index) {
        super(index);
    }

    @Override
    public void set(Matrix4f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buf = stack.callocFloat(16);
            value.get(buf);

            GL30C.glUniformMatrix4fv(this.index, false, buf);
        }
    }
}
