package me.jellysquid.mods.sodium.client.world.cloned;

import me.jellysquid.mods.sodium.client.world.cloned.palette.ClonedPalette;

public interface PackedIntegerArrayExtended {
    <T> void sodium$unpack(T[] out, ClonedPalette<T> palette);
}
