package me.jellysquid.mods.sodium.client.world.cloned;

import me.jellysquid.mods.sodium.client.world.cloned.palette.ClonedPalette;

public interface PackedIntegerArrayExtended {
    <T> void copyUsingPalette(T[] out, ClonedPalette<T> palette);
}
