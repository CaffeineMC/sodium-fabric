package me.jellysquid.mods.sodium.opengl.shader.uniform;

import org.lwjgl.opengl.GL45C;

public class UniformInt extends Uniform {
    private UniformInt(int program, int index) {
        super(program, index);
    }

    public static UniformFactory<UniformInt> of() {
        return UniformInt::new;
    }

    public void setInt(int value) {
        GL45C.glProgramUniform1i(this.program, this.index, value);
    }
}
