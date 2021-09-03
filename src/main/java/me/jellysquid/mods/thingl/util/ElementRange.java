package me.jellysquid.mods.thingl.util;

import me.jellysquid.mods.thingl.tessellation.GlIndexType;

public record ElementRange(int elementPointer, int elementCount, GlIndexType indexType, int baseVertex) {
}
