package me.jellysquid.mods.sodium.client.render.biome;

import net.minecraft.client.color.world.FoliageColors;
import net.minecraft.client.color.world.GrassColors;

public class BiomeColorMaps {
    public static int getGrassColor(int index) {
        if (index == -1) {
            return 0xffff00ff;
        }

        return GrassColors.colorMap[index];
    }

    public static int getFoliageColor(int index) {
        if (index == -1) {
            return 0xffff00ff;
        }

        return FoliageColors.colorMap[index];
    }

    public static int getIndex(double temperature, double humidity) {
        humidity *= temperature;

        int x = (int) ((1.0 - temperature) * 255.0);
        int z = (int) ((1.0 - humidity) * 255.0);

        int index = z << 8 | x;

        return index >= 65536 ? -1 : index;
    }
}
