package net.caffeinemc.gfx.api.pipeline;

import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.shader.BufferBlock;
import net.caffeinemc.gfx.api.texture.Sampler;
import net.caffeinemc.gfx.api.texture.Texture;

public interface PipelineState {
    // TODO: bind these all at once before getting the gate, don't make it state related
    void bindTexture(int unit, Texture texture, Sampler sampler);

    // TODO: split these back up into StorageBlock and UniformBlock? idk, maybe it would match the rest of the api better.
    void bindBufferBlock(BufferBlock block, Buffer buffer);

    void bindBufferBlock(BufferBlock block, Buffer buffer, long offset, long length);
}
