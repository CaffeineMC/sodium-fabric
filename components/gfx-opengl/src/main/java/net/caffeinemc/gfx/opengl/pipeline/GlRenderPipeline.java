package net.caffeinemc.gfx.opengl.pipeline;

import net.caffeinemc.gfx.api.array.VertexArray;
import net.caffeinemc.gfx.api.shader.Program;
import net.caffeinemc.gfx.api.pipeline.RenderPipelineDescription;
import net.caffeinemc.gfx.api.pipeline.RenderPipeline;

public class GlRenderPipeline<PROGRAM, ARRAY extends Enum<ARRAY>> implements RenderPipeline<PROGRAM, ARRAY> {
    private final RenderPipelineDescription state;
    private final Program<PROGRAM> program;
    private final VertexArray<ARRAY> vertexArray;

    public GlRenderPipeline(RenderPipelineDescription state, Program<PROGRAM> program, VertexArray<ARRAY> vertexArray) {
        this.state = state;
        this.program = program;
        this.vertexArray = vertexArray;
    }

    @Override
    public RenderPipelineDescription getDescription() {
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
