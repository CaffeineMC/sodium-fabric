package net.caffeinemc.mods.sodium.client.world.biome;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.ColorResolver;

/**
 * The default biome color sources which Minecraft provides. These map to the functions in
 * {@link net.minecraft.world.level.biome.Biome} for querying colors.
 *
 * <p>This exists because the indirection of invoking {@link ColorResolver} repeatedly causes performance
 * issues when blending biome colors. This enumeration provides the ability for the renderer to inline and cache
 * the color values which each biome provides.</p>
 */
public enum BiomeColorSource {
    GRASS,
    FOLIAGE,
    WATER;

    public static final BiomeColorSource[] VALUES = BiomeColorSource.values();
    public static final int COUNT = VALUES.length;

    /**
     * Maps a default {@link ColorResolver} into a typed {@link BiomeColorSource}.
     * @param resolver The generic color resolver to remap
     * @return A typed color source which describes the color resolver
     */
    public static BiomeColorSource from(ColorResolver resolver) {
        if (resolver == BiomeColors.GRASS_COLOR_RESOLVER) {
            return GRASS;
        } else if (resolver == BiomeColors.FOLIAGE_COLOR_RESOLVER) {
            return FOLIAGE;
        } else if (resolver == BiomeColors.WATER_COLOR_RESOLVER) {
            return WATER;
        }

        throw new UnsupportedOperationException();
    }
}
