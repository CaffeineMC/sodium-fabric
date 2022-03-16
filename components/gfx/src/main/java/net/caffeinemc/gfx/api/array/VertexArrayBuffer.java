package net.caffeinemc.gfx.api.array;

import net.caffeinemc.gfx.api.buffer.Buffer;

/**
 * Describes a buffer object which vertex arrays can source attributes from.
 *
 * @param offset The starting offset (in bytes) at which vertex attributes will be sourced from
 * @param stride The number of bytes between each consecutive set of attributes in the buffer
 */
public record VertexArrayBuffer(Buffer buffer, int offset, int stride) {
}
