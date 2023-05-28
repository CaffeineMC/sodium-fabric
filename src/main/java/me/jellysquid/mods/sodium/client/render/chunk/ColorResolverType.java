package me.jellysquid.mods.sodium.client.render.chunk;

import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.world.biome.ColorResolver;

public enum ColorResolverType {

    GRASS, FOLIAGE, WATER;

    public static final ColorResolverType[] VALUES = ColorResolverType.values();

    public static final int COUNT = VALUES.length;

    public static ColorResolverType get(ColorResolver resolver) {
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
