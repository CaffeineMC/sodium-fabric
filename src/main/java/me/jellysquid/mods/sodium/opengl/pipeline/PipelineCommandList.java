package me.jellysquid.mods.sodium.opengl.pipeline;

import me.jellysquid.mods.sodium.opengl.shader.Program;
import me.jellysquid.mods.sodium.opengl.shader.ProgramCommandList;

public interface PipelineCommandList {
    <T> void useProgram(Program<T> program, ProgramGate<T> gate);

    interface ProgramGate<T> {
        void run(ProgramCommandList commandList, T programInterface);
    }
}
