package me.jellysquid.mods.sodium.opengl.shader.uniform;

import org.lwjgl.opengl.GL45C;

public class UniformInt extends Uniform {
    private int currentValue;

    private UniformInt(int program, int index) {
        super(program, index);

        this.currentValue = GL45C.glGetUniformi(program, index);
    }

    public static UniformFactory<UniformInt> of() {
        return UniformInt::new;
    }

    public void setInt(int value) {
        if (this.currentValue != value) {
            GL45C.glProgramUniform1i(this.program, this.index, value);
            this.currentValue = value;
        }
    }
}
