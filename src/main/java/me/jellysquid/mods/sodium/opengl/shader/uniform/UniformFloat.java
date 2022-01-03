package me.jellysquid.mods.sodium.opengl.shader.uniform;

import org.lwjgl.opengl.GL30C;

public class UniformFloat extends Uniform<Float> {
    public UniformFloat(int index) {
        super(index);
    }

    @Override
    public void set(Float value) {
        this.setFloat(value);
    }

    public void setFloat(float value) {
        GL30C.glUniform1f(this.index, value);
    }
}
