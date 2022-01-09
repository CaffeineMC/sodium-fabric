package me.jellysquid.mods.sodium.opengl.array;

import me.jellysquid.mods.sodium.opengl.buffer.Buffer;

public record VertexArrayBuffer(Buffer buffer, int offset, int stride) {
}
