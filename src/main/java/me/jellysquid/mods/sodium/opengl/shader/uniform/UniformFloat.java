package me.jellysquid.mods.sodium.opengl.shader.uniform;

import org.lwjgl.opengl.GL45C;

public class UniformFloat extends Uniform {
    private float currentValue;

    private UniformFloat(int program, int index) {
        super(program, index);

        this.currentValue = GL45C.glGetUniformf(program, index);
    }

    public static UniformFactory<UniformFloat> of() {
        return UniformFloat::new;
    }

    public void setFloat(float value) {
        if (this.currentValue != value) {
            GL45C.glProgramUniform1f(this.program, this.index, value);

            this.currentValue = value;
        }
    }
}
