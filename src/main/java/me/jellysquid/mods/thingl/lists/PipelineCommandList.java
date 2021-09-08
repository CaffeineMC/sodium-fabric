package me.jellysquid.mods.thingl.lists;

import me.jellysquid.mods.thingl.shader.GlProgram;

public interface PipelineCommandList {
    <T> void useProgram(GlProgram<T> program, ShaderEntrypoint<T> consumer);

    interface ShaderEntrypoint<T> {
        void accept(ShaderCommandList commandList, T inf);
    }
}
