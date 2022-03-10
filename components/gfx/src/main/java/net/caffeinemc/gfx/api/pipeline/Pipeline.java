package net.caffeinemc.gfx.api.pipeline;

import net.caffeinemc.gfx.api.array.VertexArray;
import net.caffeinemc.gfx.api.shader.Program;

public interface Pipeline<PROGRAM, ARRAY extends Enum<ARRAY>> {
    PipelineDescription getDescription();

    Program<PROGRAM> getProgram();

    VertexArray<ARRAY> getVertexArray();
}
