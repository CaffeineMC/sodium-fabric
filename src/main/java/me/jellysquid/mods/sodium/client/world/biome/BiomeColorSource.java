package me.jellysquid.mods.sodium.client.world.biome;

import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.world.biome.ColorResolver;

public enum BiomeColorSource {
    GRASS,
    FOLIAGE,
    WATER;

    public static final BiomeColorSource[] VALUES = BiomeColorSource.values();
    public static final int COUNT = VALUES.length;

    public static BiomeColorSource from(ColorResolver resolver) {
        if (resolver == BiomeColors.GRASS_COLOR) {
            return GRASS;
        } else if (resolver == BiomeColors.FOLIAGE_COLOR) {
            return FOLIAGE;
        } else if (resolver == BiomeColors.WATER_COLOR) {
            return WATER;
        }

        throw new UnsupportedOperationException();
    }
}
