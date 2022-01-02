package me.jellysquid.mods.sodium.client.gl.util;

import me.jellysquid.mods.sodium.client.gl.array.GlIndexType;

public record ElementRange(int elementPointer, int elementCount, GlIndexType indexType, int baseVertex) {
}
