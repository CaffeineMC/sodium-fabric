package net.caffeinemc.mods.sodium.client.world;

import net.minecraft.world.level.chunk.Palette;

/**
 * Provides optimized functions for unpacking a {@link net.minecraft.util.BitStorage} to memory.
 */
public interface BitStorageExtension {
    /**
     * Unpacks the values of the bit array, and remaps them to palette entries.
     *
     * @param out The array to store the unpacked values into
     * @param palette The palette to use for looking up
     * @param <T> The type of values stored in the palette
     * @throws NullPointerException If the storage contains any values which map to a null entry in the palette
     */
    <T> void sodium$unpack(T[] out, Palette<T> palette);
}
