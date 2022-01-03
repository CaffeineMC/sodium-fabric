package me.jellysquid.mods.sodium.interop.vanilla.mixin;

import me.jellysquid.mods.sodium.world.cloned.palette.ClonedPalette;

public interface PackedIntegerArrayExtended {
    <T> void copyUsingPalette(T[] out, ClonedPalette<T> palette);
}
