package net.caffeinemc.gfx.api.pipeline;

import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.shader.BufferBlock;
import net.caffeinemc.gfx.api.texture.Sampler;

public interface PipelineState {
    // TODO: disallow directly passing texture handles, require validated object
    void bindTexture(int unit, int texture, Sampler sampler);

    void bindUniformBlock(BufferBlock block, Buffer buffer);

    void bindUniformBlock(BufferBlock block, Buffer buffer, long offset, long length);
}
