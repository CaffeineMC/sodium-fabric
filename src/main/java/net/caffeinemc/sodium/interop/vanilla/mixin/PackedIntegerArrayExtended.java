package net.caffeinemc.sodium.interop.vanilla.mixin;

import net.caffeinemc.sodium.world.slice.cloned.palette.ClonedPalette;

public interface PackedIntegerArrayExtended {
    <T> void copyUsingPalette(T[] out, ClonedPalette<T> palette);
}
