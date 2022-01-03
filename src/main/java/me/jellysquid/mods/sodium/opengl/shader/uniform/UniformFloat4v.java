package me.jellysquid.mods.sodium.opengl.shader.uniform;

import org.lwjgl.opengl.GL30C;

public class UniformFloat4v extends Uniform<float[]> {
    public UniformFloat4v(int index) {
        super(index);
    }

    @Override
    public void set(float[] value) {
        if (value.length != 4) {
            throw new IllegalArgumentException("value.length != 4");
        }

        GL30C.glUniform4fv(this.index, value);
    }
}
