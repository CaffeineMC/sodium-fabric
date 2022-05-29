package net.caffeinemc.gfx.opengl.shader.uniform;

import net.caffeinemc.gfx.api.shader.BufferBlockType;
import net.caffeinemc.gfx.api.shader.Program;
import net.caffeinemc.gfx.api.shader.BufferBlock;

public class GlBufferBlock implements BufferBlock {
    private final Program<?> program;
    private final BufferBlockType type;
    private final int bindingIndex;

    public GlBufferBlock(Program<?> program, BufferBlockType type, int bindingIndex) {
        this.program = program;
        this.type = type;
        this.bindingIndex = bindingIndex;
    }

    @Override
    public BufferBlockType type() {
        return this.type;
    }

    @Override
    public int index() {
        return this.bindingIndex;
    }
}
