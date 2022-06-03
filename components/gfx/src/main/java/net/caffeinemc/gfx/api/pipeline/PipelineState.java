package net.caffeinemc.gfx.api.pipeline;

import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.shader.BufferBlock;
import net.caffeinemc.gfx.api.texture.Sampler;

public interface PipelineState {
    // TODO: disallow directly passing texture handles, require validated object
    void bindTexture(int unit, int texture, Sampler sampler);

    // TODO: split these back up into StorageBlock and UniformBlock? idk, maybe it would match the rest of the api better.
    void bindBufferBlock(BufferBlock block, Buffer buffer);

    void bindBufferBlock(BufferBlock block, Buffer buffer, long offset, long length);
}
