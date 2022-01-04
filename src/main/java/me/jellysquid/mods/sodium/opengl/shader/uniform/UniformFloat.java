package me.jellysquid.mods.sodium.opengl.shader.uniform;

import org.lwjgl.opengl.GL45C;

public class UniformFloat extends Uniform {
    private UniformFloat(int program, int index) {
        super(program, index);
    }

    public static UniformFactory<UniformFloat> of() {
        return UniformFloat::new;
    }

    public void setFloat(float value) {
        GL45C.glProgramUniform1f(this.program, this.index, value);
    }
}
