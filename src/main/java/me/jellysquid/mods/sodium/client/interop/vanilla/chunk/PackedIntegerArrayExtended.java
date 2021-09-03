package me.jellysquid.mods.sodium.client.interop.vanilla.chunk;

import me.jellysquid.mods.sodium.client.world.cloned.palette.ClonedPalette;

public interface PackedIntegerArrayExtended {
    <T> void copyUsingPalette(T[] out, ClonedPalette<T> palette);
}
