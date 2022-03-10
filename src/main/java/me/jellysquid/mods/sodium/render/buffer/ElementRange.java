package me.jellysquid.mods.sodium.render.buffer;

import net.caffeinemc.gfx.api.types.IntType;

public record ElementRange(int firstIndex, int elementCount, IntType indexType, int baseVertex) {
}
