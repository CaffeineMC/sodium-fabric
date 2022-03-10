package net.caffeinemc.gfx.api.shader;

import net.caffeinemc.gfx.api.buffer.Buffer;

public interface UniformBlock {
    void bindBuffer(Buffer buffer);

    void bindBuffer(Buffer buffer, int offset, long length);
}
