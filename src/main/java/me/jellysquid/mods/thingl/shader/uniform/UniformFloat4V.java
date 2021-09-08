package me.jellysquid.mods.thingl.shader.uniform;

import org.lwjgl.opengl.GL30C;

public class UniformFloat4V extends Uniform<float[]> {
    public UniformFloat4V(int index) {
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
