package net.caffeinemc.mods.sodium.client.world;

import net.minecraft.world.level.chunk.Palette;

public interface BitStorageExtension {
    <T> void sodium$unpack(T[] out, Palette<T> palette);
}
