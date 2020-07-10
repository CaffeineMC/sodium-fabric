package me.jellysquid.mods.sodium.client.world.biome;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.level.ColorResolver;

import java.util.Arrays;

public class BiomeColorBlendCache {
    private final ColorResolver resolver;
    private final WorldSlice slice;

    private final int[] blendedColors;
    private final int[] cache;

    private final int radius;
    private final int length;
    private final int minX, minZ;

    public BiomeColorBlendCache(ColorResolver resolver, WorldSlice slice) {
        this.resolver = resolver;
        this.slice = slice;
        this.radius = SodiumClientMod.options().quality.biomeBlendDistance;

        this.minX = slice.getBlockOffsetX() - this.radius - 2;
        this.minZ = slice.getBlockOffsetZ() - this.radius - 2;

        this.length = WorldSlice.BLOCK_LENGTH + ((this.radius + 2) * 2);

        this.cache = new int[this.length * this.length];
        this.blendedColors = new int[this.length * this.length];

        Arrays.fill(this.cache, -1);
        Arrays.fill(this.blendedColors, -1);
    }

    public int getBlendedColor(BlockPos pos) {
        int index = ((pos.getX() - this.minX) * this.length) + (pos.getZ() - this.minZ);
        int color = this.blendedColors[index];

        if (color == -1) {
            this.blendedColors[index] = color = this.calculateBlendedColor(pos.getX(), pos.getZ());
        }

        return color;
    }

    private int calculateBlendedColor(int posX, int posZ) {
        if (this.radius == 0) {
            return this.getColor(posX, posZ);
        }

        int diameter = (this.radius * 2) + 1;
        int area = diameter * diameter;

        int r = 0;
        int g = 0;
        int b = 0;

        int minX = posX - this.radius;
        int minZ = posZ - this.radius;

        int maxX = posX + this.radius;
        int maxZ = posZ + this.radius;

        for (int x2 = minX; x2 <= maxX; x2++) {
            for (int z2 = minZ; z2 <= maxZ; z2++) {
                int color = this.getColor(x2, z2);

                r += ColorARGB.unpackRed(color);
                g += ColorARGB.unpackGreen(color);
                b += ColorARGB.unpackBlue(color);
            }
        }

        return ColorARGB.pack(r / area, g / area, b / area, 255);
    }

    private int getColor(int x, int z) {
        int index = ((x - this.minX) * this.length) + (z - this.minZ);
        int color = this.cache[index];

        if (color == -1) {
            this.cache[index] = color = this.calculateColor(x, z);
        }

        return color;
    }

    private int calculateColor(int x, int z) {
        return this.resolver.getColor(this.slice.getCachedBiome(x, z), x, z);
    }
}
