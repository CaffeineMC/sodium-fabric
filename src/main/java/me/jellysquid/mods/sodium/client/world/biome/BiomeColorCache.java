package me.jellysquid.mods.sodium.client.world.biome;

import me.jellysquid.mods.sodium.client.util.color.BoxBlur;
import me.jellysquid.mods.sodium.client.util.color.BoxBlur.ColorBuffer;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;

import java.util.Arrays;

public class BiomeColorCache {
    private static final int NEIGHBOR_BLOCK_RADIUS = 2;
    private final BiomeSlice biomeData;

    private final Slice[] slices;
    private final boolean[] populatedSlices;

    private final int blendRadius;

    private final ColorBuffer tempColorBuffer;

    private int minX, minY, minZ;
    private int maxX, maxY, maxZ;

    public BiomeColorCache(BiomeSlice biomeData, int blendRadius) {
        this.biomeData = biomeData;
        this.blendRadius = blendRadius;

        int sizeXZ = 16 + ((NEIGHBOR_BLOCK_RADIUS + this.blendRadius) * 2);
        int sizeY = 16 + (NEIGHBOR_BLOCK_RADIUS * 2);

        this.slices = new Slice[sizeY];
        this.populatedSlices = new boolean[sizeY];

        for (int y = 0; y < sizeY; y++) {
            this.slices[y] = new Slice(sizeXZ);
        }

        this.tempColorBuffer = new ColorBuffer(sizeXZ, sizeXZ);
    }

    public void update(ChunkRenderContext context) {
        this.minX = (context.getOrigin().getMinX() - NEIGHBOR_BLOCK_RADIUS) - this.blendRadius;
        this.minY = (context.getOrigin().getMinY() - NEIGHBOR_BLOCK_RADIUS);
        this.minZ = (context.getOrigin().getMinZ() - NEIGHBOR_BLOCK_RADIUS) - this.blendRadius;

        this.maxX = (context.getOrigin().getMaxX() + NEIGHBOR_BLOCK_RADIUS) + this.blendRadius;
        this.maxY = (context.getOrigin().getMaxY() + NEIGHBOR_BLOCK_RADIUS);
        this.maxZ = (context.getOrigin().getMaxZ() + NEIGHBOR_BLOCK_RADIUS) + this.blendRadius;

        Arrays.fill(this.populatedSlices, false);
    }

    public int getColor(BiomeColorSource source, int blockX, int blockY, int blockZ) {
        var relX = MathHelper.clamp(blockX, this.minX, this.maxX) - this.minX;
        var relY = MathHelper.clamp(blockY, this.minY, this.maxY) - this.minY;
        var relZ = MathHelper.clamp(blockZ, this.minZ, this.maxZ) - this.minZ;

        if (!this.populatedSlices[relY]) {
            this.updateColorBuffers(relY);
        }

        var slice = this.slices[relY];
        var buffer = slice.getBuffer(source);

        return buffer.get(relX, relZ);
    }

    private void updateColorBuffers(int relY) {
        var slice = this.slices[relY];

        int worldY = this.minY + relY;

        for (int worldZ = this.minZ; worldZ <= this.maxZ; worldZ++) {
            for (int worldX = this.minX; worldX <= this.maxX; worldX++) {
                Biome biome = this.biomeData.getBiome(worldX, worldY, worldZ).value();

                int relativeX = worldX - this.minX;
                int relativeZ = worldZ - this.minZ;

                slice.grass.set(relativeX, relativeZ, BiomeColors.GRASS_COLOR.getColor(biome, worldX, worldZ));
                slice.foliage.set(relativeX, relativeZ, BiomeColors.FOLIAGE_COLOR.getColor(biome, worldX, worldZ));
                slice.water.set(relativeX, relativeZ, BiomeColors.WATER_COLOR.getColor(biome, worldX, worldZ));
            }
        }

        if (this.blendRadius > 0) {
            BoxBlur.blur(slice.grass, this.tempColorBuffer, this.blendRadius);
            BoxBlur.blur(slice.foliage, this.tempColorBuffer, this.blendRadius);
            BoxBlur.blur(slice.water, this.tempColorBuffer, this.blendRadius);
        }

        this.populatedSlices[relY] = true;
    }

    private static class Slice {
        private final ColorBuffer grass;
        private final ColorBuffer foliage;
        private final ColorBuffer water;

        private Slice(int size) {
            this.grass = new ColorBuffer(size, size);
            this.foliage = new ColorBuffer(size, size);
            this.water = new ColorBuffer(size, size);
        }

        public ColorBuffer getBuffer(BiomeColorSource source) {
            return switch (source) {
                case GRASS -> this.grass;
                case FOLIAGE -> this.foliage;
                case WATER -> this.water;
            };
        }
    }
}
