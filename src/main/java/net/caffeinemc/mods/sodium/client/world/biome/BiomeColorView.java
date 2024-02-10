package net.caffeinemc.mods.sodium.client.world.biome;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ColorResolver;

/**
 * Provides access to biome colors within the level.
 */
public interface BiomeColorView {
    /**
     * Samples the biome color source for the given block coordinate. The color source is blended across a radius
     * to produce smoother results. The results of this function should be identical to
     * {@link ClientLevel#calculateBlockTint(BlockPos, ColorResolver)}.
     *
     * @param source The color source to use for sampling
     * @param blockX The x-coordinate of the block position
     * @param blockY The Y-coordinate of the block position
     * @param blockZ The Z-coordinate of the block position
     * @return A color value in ARGB-format for the block coordinate
     */
    int getColor(BiomeColorSource source, int blockX, int blockY, int blockZ);
}
