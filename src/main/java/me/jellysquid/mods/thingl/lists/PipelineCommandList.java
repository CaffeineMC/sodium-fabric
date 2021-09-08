package me.jellysquid.mods.thingl.lists;

import me.jellysquid.mods.thingl.shader.Program;

public interface PipelineCommandList {
    <T> void useProgram(Program<T> program, ShaderEntrypoint<T> consumer);

    interface ShaderEntrypoint<T> {
        void accept(ShaderCommandList commandList, T inf);
    }
}
