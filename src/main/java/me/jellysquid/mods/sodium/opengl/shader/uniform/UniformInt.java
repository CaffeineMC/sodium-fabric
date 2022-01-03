package me.jellysquid.mods.sodium.opengl.shader.uniform;

import org.lwjgl.opengl.GL30C;

public class UniformInt extends Uniform<Integer> {
    public UniformInt(int index) {
        super(index);
    }

    @Override
    public void set(Integer value) {
        this.setInt(value);
    }

    public void setInt(int value) {
        GL30C.glUniform1i(this.index, value);
    }
}
