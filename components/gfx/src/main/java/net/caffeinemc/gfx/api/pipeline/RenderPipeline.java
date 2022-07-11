package net.caffeinemc.gfx.api.pipeline;

import net.caffeinemc.gfx.api.array.VertexArray;
import net.caffeinemc.gfx.api.shader.Program;

public interface RenderPipeline<PROGRAM, ARRAY extends Enum<ARRAY>> {
    Program<PROGRAM> getProgram();
    
    RenderPipelineDescription getDescription();

    VertexArray<ARRAY> getVertexArray();
}
