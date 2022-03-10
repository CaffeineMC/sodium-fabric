package net.caffeinemc.gfx.api.array;

import net.caffeinemc.gfx.api.buffer.Buffer;

public record VertexArrayBuffer(Buffer buffer, int offset, int stride) {
}
