package me.jellysquid.mods.sodium.opengl.pipeline;

import me.jellysquid.mods.sodium.opengl.array.VertexArray;
import me.jellysquid.mods.sodium.opengl.shader.Program;
import me.jellysquid.mods.sodium.opengl.types.RenderState;

public class PipelineImpl<PROGRAM, ARRAY extends Enum<ARRAY>> implements Pipeline<PROGRAM, ARRAY> {
    private final RenderState state;
    private final Program<PROGRAM> program;
    private final VertexArray<ARRAY> vertexArray;

    public PipelineImpl(RenderState state, Program<PROGRAM> program, VertexArray<ARRAY> vertexArray) {
        this.state = state;
        this.program = program;
        this.vertexArray = vertexArray;
    }

    @Override
    public RenderState getState() {
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
