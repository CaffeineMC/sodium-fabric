package net.caffeinemc.mods.sodium.client.world.biome;

import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;

/**
 * The default color maps which are provided by Minecraft. Color maps are addressed on two axis, the humidity (X-axis)
 * and temperature (Y-axis).
 */
public class BiomeColorMaps {
    /**
     * The width of each biome color map texture.
     */
    private static final int WIDTH = 256;

    /**
     * The height of each biome color map texture.
     */
    private static final int HEIGHT = 256;

    /**
     * The sentinel value which is returned when invalid coordinates are passed to {@link #getIndex(double, double)}.
     */
    private static final int INVALID_INDEX = -1;

    /**
     * The default color returned by biome color maps when {@link #INVALID_INDEX} is used to access them.
     */
    private static final int DEFAULT_COLOR = 0xffff00ff;

    /**
     * Computes a texel index on the color map texture from the given temperature and humidity. This value can then
     * be used to query the color from each biome color map.
     *
     * @param temperature The value on the temperature axis
     * @param humidity The value on the humidity axis
     * @return An index in the color map array, or {@link BiomeColorMaps#INVALID_INDEX} if {@param temperature} or
     *         {@param humidity} were outside the allowed value range
     */
    public static int getIndex(double temperature, double humidity) {
        humidity *= temperature;

        int x = (int) ((1.0D - temperature) * 255.0D);
        int y = (int) ((1.0D - humidity) * 255.0D);

        if (x < 0 || x >= WIDTH) {
            return INVALID_INDEX;
        }

        if (y < 0 || y >= HEIGHT) {
            return INVALID_INDEX;
        }

        return (y << 8) | x;
    }

    /**
     * @param index The texel index computed from {@link BiomeColorMaps#getIndex(double, double)}
     * @return The color of grass for the given texel index, or the default color if the index is invalid
     */
    public static int getGrassColor(int index) {
        if (index == INVALID_INDEX) {
            return DEFAULT_COLOR;
        }

        return GrassColor.pixels[index];
    }

    /**
     * @param index The texel index computed from {@link BiomeColorMaps#getIndex(double, double)}
     * @return The color of grass for the given texel index, or the default color if the index is invalid
     */
    public static int getFoliageColor(int index) {
        if (index == INVALID_INDEX) {
            return DEFAULT_COLOR;
        }

        return FoliageColor.pixels[index];
    }
}
