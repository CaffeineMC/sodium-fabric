package me.jellysquid.mods.sodium.client.world.biome;

import net.minecraft.client.color.world.FoliageColors;
import net.minecraft.client.color.world.GrassColors;

public class BiomeColorMaps {
    private static final int WIDTH = 256;
    private static final int HEIGHT = 256;

    private static final int INVALID_INDEX = -1;
    private static final int DEFAULT_COLOR = 0xffff00ff;

    public static int getGrassColor(int index) {
        if (index == INVALID_INDEX) {
            return DEFAULT_COLOR;
        }

        return GrassColors.colorMap[index];
    }

    public static int getFoliageColor(int index) {
        if (index == INVALID_INDEX) {
            return DEFAULT_COLOR;
        }

        return FoliageColors.colorMap[index];
    }

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
}
