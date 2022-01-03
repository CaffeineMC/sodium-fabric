package me.jellysquid.mods.sodium.render.buffer;

import me.jellysquid.mods.sodium.opengl.types.IntType;

public record ElementRange(int elementPointer, int elementCount, IntType indexType, int baseVertex) {
}
