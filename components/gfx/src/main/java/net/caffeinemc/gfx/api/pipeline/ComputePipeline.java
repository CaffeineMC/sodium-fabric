package net.caffeinemc.gfx.api.pipeline;

import net.caffeinemc.gfx.api.shader.Program;

public interface ComputePipeline<PROGRAM> {
    Program<PROGRAM> getProgram();
}
