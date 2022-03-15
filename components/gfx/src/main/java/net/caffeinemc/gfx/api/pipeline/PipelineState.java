package net.caffeinemc.gfx.api.pipeline;

import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.shader.UniformBlock;
import net.caffeinemc.gfx.api.texture.Sampler;

public interface PipelineState {
    // TODO: disallow directly passing texture handles, require validated object
    void bindTexture(int unit, int texture, Sampler sampler);

    void bindUniformBlock(UniformBlock block, Buffer buffer);

    void bindUniformBlock(UniformBlock block, Buffer buffer, long offset, long length);
}
