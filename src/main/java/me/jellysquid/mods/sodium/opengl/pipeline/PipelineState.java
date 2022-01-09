package me.jellysquid.mods.sodium.opengl.pipeline;

import me.jellysquid.mods.sodium.opengl.sampler.Sampler;

public interface PipelineState {
    // TODO: disallow directly passing texture handles, require validated object
    void bindTexture(int unit, int texture, Sampler sampler);
}
