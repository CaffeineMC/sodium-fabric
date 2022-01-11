package me.jellysquid.mods.sodium.render.buffer;

import me.jellysquid.mods.sodium.opengl.types.IntType;

public record ElementRange(int firstIndex, int elementCount, IntType indexType, int baseVertex) {
}
