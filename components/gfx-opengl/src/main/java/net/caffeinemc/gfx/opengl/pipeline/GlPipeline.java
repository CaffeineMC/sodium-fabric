package net.caffeinemc.gfx.opengl.pipeline;

import net.caffeinemc.gfx.api.array.VertexArray;
import net.caffeinemc.gfx.api.shader.Program;
import net.caffeinemc.gfx.api.pipeline.PipelineDescription;
import net.caffeinemc.gfx.api.pipeline.Pipeline;

public class GlPipeline<PROGRAM, ARRAY extends Enum<ARRAY>> implements Pipeline<PROGRAM, ARRAY> {
    private final PipelineDescription state;
    private final Program<PROGRAM> program;
    private final VertexArray<ARRAY> vertexArray;

    public GlPipeline(PipelineDescription state, Program<PROGRAM> program, VertexArray<ARRAY> vertexArray) {
        this.state = state;
        this.program = program;
        this.vertexArray = vertexArray;
    }

    @Override
    public PipelineDescription getDescription() {
        return this.state;
    }

    @Override
    public Program<PROGRAM> getProgram() {
        return this.program;
    }

    @Override
    public VertexArray<ARRAY> getVertexArray() {
        return this.vertexArray;
    }
}
