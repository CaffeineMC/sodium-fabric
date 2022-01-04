package me.jellysquid.mods.sodium.opengl.shader.uniform;

public abstract class Uniform {
    protected final int program;
    protected final int index;

    protected Uniform(int program, int index) {
        this.program = program;
        this.index = index;
    }
}
