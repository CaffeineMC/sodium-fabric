package net.caffeinemc.sodium.render.buffer;

import net.caffeinemc.gfx.api.types.ElementFormat;

public record ElementRange(int firstIndex, int elementCount, ElementFormat indexType, int baseVertex) {
}
