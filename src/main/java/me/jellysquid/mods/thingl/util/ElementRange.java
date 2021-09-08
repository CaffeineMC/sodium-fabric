package me.jellysquid.mods.thingl.util;

import me.jellysquid.mods.thingl.tessellation.IndexType;

public record ElementRange(int elementPointer, int elementCount, IndexType indexType, int baseVertex) {
}
