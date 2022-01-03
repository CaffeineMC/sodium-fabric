package me.jellysquid.mods.sodium.opengl.shader.uniform;

import org.lwjgl.opengl.GL30C;

public class UniformFloat3v extends Uniform<float[]> {
    public UniformFloat3v(int index) {
        super(index);
    }

    @Override
    public void set(float[] value) {
        if (value.length != 3) {
            throw new IllegalArgumentException("value.length != 3");
        }

        GL30C.glUniform3fv(this.index, value);
    }

    public void set(float x, float y, float z) {
        GL30C.glUniform3f(this.index, x, y, z);
    }
}
