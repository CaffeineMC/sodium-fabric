package me.jellysquid.mods.sodium.client.world;

import net.minecraft.world.level.chunk.Palette;

public interface PaletteStorageExtended {
    <T> void sodium$unpack(T[] out, Palette<T> palette);
}
