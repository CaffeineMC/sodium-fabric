package net.caffeinemc.gfx.opengl.shader.uniform;

import net.caffeinemc.gfx.api.shader.Program;
import net.caffeinemc.gfx.api.shader.BufferBlock;

public class GlBufferBlock implements BufferBlock {
    private final Program<?> program;
    private final int binding;

    public GlBufferBlock(Program<?> program, int bufferBlock) {
        this.program = program;
        this.binding = bufferBlock;
    }

    @Override
    public int index() {
        return this.binding;
    }
}
