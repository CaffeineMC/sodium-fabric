package net.caffeinemc.gfx.opengl.shader.uniform;

import net.caffeinemc.gfx.api.shader.Program;
import net.caffeinemc.gfx.api.shader.UniformBlock;

// TODO: do not allow this type to bind itself, instead require the pipeline gate to bind ubos
public class GlUniformBlock implements UniformBlock {
    private final Program<?> program;
    private final int binding;

    public GlUniformBlock(Program<?> program, int uniformBlockBinding) {
        this.program = program;
        this.binding = uniformBlockBinding;
    }

    @Override
    public int binding() {
        return this.binding;
    }
}
