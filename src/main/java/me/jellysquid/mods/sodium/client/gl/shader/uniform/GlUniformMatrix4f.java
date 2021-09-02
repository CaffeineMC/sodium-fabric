package me.jellysquid.mods.sodium.client.gl.shader.uniform;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

public class GlUniformMatrix4f extends GlUniform<Matrix4f>  {
    public GlUniformMatrix4f(int index) {
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
