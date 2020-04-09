package me.jellysquid.mods.sodium.client.world.biome;

import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.level.ColorResolver;

import java.util.Arrays;

public class ColorizerCache {
    private final ColorResolver resolver;
    private final WorldSlice slice;

    private final int[] colors;
    private final int radius;

    public ColorizerCache(ColorResolver resolver, WorldSlice slice) {
        this.resolver = resolver;
        this.slice = slice;
        this.radius = MinecraftClient.getInstance().options.biomeBlendRadius;

        this.colors = new int[16 * 16];

        Arrays.fill(this.colors, -1);
    }

    public int getColor(BlockPos pos) {
        int x = pos.getX() & 15;
        int z = pos.getZ() & 15;

        int idx = z << 4 | x;
        int color = this.colors[idx];

        if (color == -1) {
            this.colors[idx] = color = this.calculateColor(pos);
        }

        return color;
    }

    public int calculateColor(BlockPos pos) {
        if (this.radius == 0) {
            return this.resolver.getColor(this.getBiome(pos), pos.getX(), pos.getZ());
        }

        int diameter = (this.radius * 2 + 1);
        int area = diameter * diameter;

        int r = 0;
        int g = 0;
        int b = 0;

        int minX = pos.getX() - this.radius;
        int minZ = pos.getZ() - this.radius;

        int maxX = pos.getX() + this.radius;
        int maxZ = pos.getZ() + this.radius;

        int y = pos.getY();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int color = this.resolver.getColor(this.getBiome(x, y, z), x, z);

                r += (color & 0xFF0000) >> 16;
                g += (color & 0x00FF00) >> 8;
                b += (color & 0x0000FF);
            }
        }

        return (((r / area) & 255) << 16) | (((g / area) & 255) << 8) | ((b / area) & 255);
    }

    private Biome getBiome(BlockPos pos) {
        return this.getBiome(pos.getX(), pos.getY(), pos.getZ());
    }

    private Biome getBiome(int x, int y, int z) {
        return this.slice.getCachedBiome(x, y, z);
    }

}
