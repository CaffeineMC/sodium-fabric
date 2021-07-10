package me.jellysquid.mods.sodium.client.gl.util;

import me.jellysquid.mods.sodium.client.gl.tessellation.GlIndexType;

public record ElementRange(int elementPointer, int elementCount, GlIndexType indexType, int baseVertex) {
}
