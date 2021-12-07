package me.jellysquid.mods.sodium.client.world.biome;

import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.level.ColorResolver;

import java.util.Arrays;

public class BiomeColorCache {
    // The maximum distance a vertex is allowed to reach beyond its origin when sampling blended biome colours.
    // The default value of 2 should suffice for complex models without degrading quality.
    private static final int MODEL_RADIUS = 2;

    private static final int BLENDED_COLORS_DIM = 16 + (MODEL_RADIUS * 2);

    private final ColorResolver resolver;
    private final WorldSlice slice;

    private final int[] cache;
    private final int[] blendedColorsZ;
    private final int[] blendedColorsXZ;
    private final int[] blendedColorsXYZ;

    private final int radius;
    private final int diameter;
    private final int dim;

    private final int minX, minY, minZ;

    private final int blendedColorsMinX;
    private final int blendedColorsMinY;
    private final int blendedColorsMinZ;

    public BiomeColorCache(ColorResolver resolver, WorldSlice slice) {
        this.resolver = resolver;
        this.slice = slice;
        this.radius = MinecraftClient.getInstance().options.biomeBlendRadius;

        ChunkSectionPos origin = this.slice.getOrigin();

        this.minX = origin.getMinX() - (this.radius + MODEL_RADIUS);
        this.minY = origin.getMinY() - (this.radius + MODEL_RADIUS);
        this.minZ = origin.getMinZ() - (this.radius + MODEL_RADIUS);

        this.dim = 16 + ((this.radius + MODEL_RADIUS) * 2);

        this.blendedColorsMinX = origin.getMinX() - MODEL_RADIUS;
        this.blendedColorsMinY = origin.getMinY() - MODEL_RADIUS;
        this.blendedColorsMinZ = origin.getMinZ() - MODEL_RADIUS;

        this.cache = new int[this.dim * this.dim * this.dim];
        this.blendedColorsZ = new int[this.dim * this.dim * BLENDED_COLORS_DIM];
        this.blendedColorsXZ = new int[this.dim * BLENDED_COLORS_DIM * BLENDED_COLORS_DIM];
        this.blendedColorsXYZ = new int[BLENDED_COLORS_DIM * BLENDED_COLORS_DIM * BLENDED_COLORS_DIM];

        // We only need to calculate the diameter as we divide after blending along each axis.
        // This does result in a minor loss of accuracy compared to dividing the accumulated colour of the entire area,
        // but the results are indistinguishable to the human eye.
        this.diameter = (this.radius * 2) + 1;

        Arrays.fill(this.cache, -1);
        Arrays.fill(this.blendedColorsZ, -1);
        Arrays.fill(this.blendedColorsXZ, -1);
        Arrays.fill(this.blendedColorsXYZ, -1);
    }

    public int getBlendedColor(BlockPos pos) {

        // Colours have been blended on all axis.
        int x2 = pos.getX() - this.blendedColorsMinX;
        int y2 = pos.getY() - this.blendedColorsMinY;
        int z2 = pos.getZ() - this.blendedColorsMinZ;

        int index = (((y2 * BLENDED_COLORS_DIM) + x2) * BLENDED_COLORS_DIM) + z2;
        int color = this.blendedColorsXYZ[index];

        if (color == -1) {
            this.blendedColorsXYZ[index] = color = this.calculateBlendedColor(pos.getX(), pos.getY(), pos.getZ());
        }

        return color;
    }

    private int calculateBlendedColor(int posX, int posY, int posZ) {
        if (this.radius == 0) {
            return this.getColor(posX, posY, posZ);
        }

        int r = 0;
        int g = 0;
        int b = 0;

        int minY = posY - this.radius;

        int maxY = posY + this.radius;

        // Blend across the Y axis using colours blended across both the X axis and Z axis.
        for (int y2 = minY; y2 <= maxY; y2++) {
            int color = this.getBlendedColorXZ(posX, y2, posZ);

            r += ColorARGB.unpackRed(color);
            g += ColorARGB.unpackGreen(color);
            b += ColorARGB.unpackBlue(color);
        }

        return ColorARGB.pack(r / this.diameter,g / this.diameter, b / this.diameter, 255);
    }

    private int getBlendedColorXZ(int x, int y, int z) {

        // Colours have not been blended on the Y axis
        int x2 = x - this.blendedColorsMinX;
        int y2 = y - this.minY;
        int z2 = z - this.blendedColorsMinZ;

        int index = (((y2 * BLENDED_COLORS_DIM) + x2) * BLENDED_COLORS_DIM) + z2;
        int color = this.blendedColorsXZ[index];

        if (color == -1) {
            this.blendedColorsXZ[index] = color = this.calculateBlendedColorXZ(x, y, z);
        }

        return color;
    }

    private int calculateBlendedColorXZ(int posX, int posY, int posZ) {

        int r = 0;
        int g = 0;
        int b = 0;

        int minX = posX - this.radius;

        int maxX = posX + this.radius;

        // Blend across the X axis using colours blended across the Z axis.
        for (int x2 = minX; x2 <= maxX; x2++) {
            int color = this.getBlendedColorZ(x2, posY, posZ);

            r += ColorARGB.unpackRed(color);
            g += ColorARGB.unpackGreen(color);
            b += ColorARGB.unpackBlue(color);
        }

        return ColorARGB.pack(r / this.diameter, g / this.diameter, b / this.diameter, 255);
    }

    private int getBlendedColorZ(int x, int y, int z) {

        // Colours have not been blended on the X and Y axis.
        int x2 = x - this.minX;
        int y2 = y - this.minY;
        int z2 = z - this.blendedColorsMinZ;

        int index = (((y2 * this.dim) + x2) * BLENDED_COLORS_DIM) + z2;
        int color = this.blendedColorsZ[index];

        if (color == -1) {
            this.blendedColorsZ[index] = color = this.calculateBlendedColorZ(x, y, z);
        }

        return color;
    }
    private int calculateBlendedColorZ(int posX, int posY, int posZ) {

        int r = 0;
        int g = 0;
        int b = 0;

        int minZ = posZ - this.radius;

        int maxZ = posZ + this.radius;

        // Blend across the Z axis using colours that have not been blended.
        for (int z2 = minZ; z2 <= maxZ; z2++) {
            int color = this.getColor(posX, posY, z2);

            r += ColorARGB.unpackRed(color);
            g += ColorARGB.unpackGreen(color);
            b += ColorARGB.unpackBlue(color);
        }

        return ColorARGB.pack(r / this.diameter, g / this.diameter, b / this.diameter, 255);
    }

    private int getColor(int x, int y, int z) {

        int x2 = x - this.minX;
        int y2 = y - this.minY;
        int z2 = z - this.minZ;

        int index = (((y2 * this.dim) + x2) * this.dim) + z2;
        int color = this.cache[index];

        if (color == -1) {
            this.cache[index] = color = this.calculateColor(x, y, z);
        }

        return color;
    }

    private int calculateColor(int x, int y, int z) {
        return this.resolver.getColor(this.slice.getBiome(x, y, z), x, z);
    }
}
