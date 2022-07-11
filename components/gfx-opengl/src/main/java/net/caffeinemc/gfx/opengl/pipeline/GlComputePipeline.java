package net.caffeinemc.gfx.opengl.pipeline;

import net.caffeinemc.gfx.api.pipeline.ComputePipeline;
import net.caffeinemc.gfx.api.shader.Program;

public class GlComputePipeline<PROGRAM> implements ComputePipeline<PROGRAM> {
    private final Program<PROGRAM> program;
    
    public GlComputePipeline(Program<PROGRAM> program) {
        this.program = program;
    }
    
    @Override
    public Program<PROGRAM> getProgram() {
        return program;
    }
}
