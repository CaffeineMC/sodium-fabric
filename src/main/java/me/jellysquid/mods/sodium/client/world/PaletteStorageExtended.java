package me.jellysquid.mods.sodium.client.world;

import net.minecraft.world.chunk.Palette;

public interface PaletteStorageExtended {
    <T> void sodium$unpack(T[] out, Palette<T> palette);
}
