package me.jellysquid.mods.sodium.render.chunk.buffer;

import me.jellysquid.mods.sodium.opengl.array.GlIndexType;

public record ElementRange(int elementPointer, int elementCount, GlIndexType indexType, int baseVertex) {
}
