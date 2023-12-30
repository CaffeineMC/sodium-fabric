package me.jellysquid.mods.sodium.client.world.biome;

import me.jellysquid.mods.sodium.client.util.color.BoxBlur;
import me.jellysquid.mods.sodium.client.util.color.BoxBlur.ColorBuffer;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.ColorResolver;

import java.util.Arrays;
import java.util.HashMap;

public class BiomeColorCache {
    private static final int NEIGHBOR_BLOCK_RADIUS = 2;
    private final BiomeSlice biomeData;

    private final HashMap<ColorResolver, Slice[]> slices;
    private final HashMap<ColorResolver, boolean[]> populatedSlices;

    private final int blendRadius;

    private final ColorBuffer tempColorBuffer;

    private int minX, minY, minZ;
    private int maxX, maxY, maxZ;

    private final int sizeXZ, sizeY;

    public BiomeColorCache(BiomeSlice biomeData, int blendRadius) {
        this.biomeData = biomeData;
        this.blendRadius = blendRadius;

        this.sizeXZ = 16 + ((NEIGHBOR_BLOCK_RADIUS + this.blendRadius) * 2);
        this.sizeY = 16 + (NEIGHBOR_BLOCK_RADIUS * 2);

        this.slices = new HashMap<>();
        this.populatedSlices = new HashMap<>();

        this.tempColorBuffer = new ColorBuffer(this.sizeXZ, this.sizeXZ);
    }

    public void update(ChunkRenderContext context) {
        this.minX = (context.getOrigin().getMinX() - NEIGHBOR_BLOCK_RADIUS) - this.blendRadius;
        this.minY = (context.getOrigin().getMinY() - NEIGHBOR_BLOCK_RADIUS);
        this.minZ = (context.getOrigin().getMinZ() - NEIGHBOR_BLOCK_RADIUS) - this.blendRadius;

        this.maxX = (context.getOrigin().getMaxX() + NEIGHBOR_BLOCK_RADIUS) + this.blendRadius;
        this.maxY = (context.getOrigin().getMaxY() + NEIGHBOR_BLOCK_RADIUS);
        this.maxZ = (context.getOrigin().getMaxZ() + NEIGHBOR_BLOCK_RADIUS) + this.blendRadius;

        for (boolean[] p : this.populatedSlices.values()) {
            Arrays.fill(p, false);
        }
    }

    public int getColor(BiomeColorSource source, int blockX, int blockY, int blockZ) {
        return switch (source) {
            case GRASS -> getColor(BiomeColors.GRASS_COLOR, blockX, blockY, blockZ);
            case FOLIAGE -> getColor(BiomeColors.FOLIAGE_COLOR, blockX, blockY, blockZ);
            case WATER -> getColor(BiomeColors.WATER_COLOR, blockX, blockY, blockZ);
        };
    }

    public int getColor(ColorResolver resolver, int blockX, int blockY, int blockZ) {
        var relX = MathHelper.clamp(blockX, this.minX, this.maxX) - this.minX;
        var relY = MathHelper.clamp(blockY, this.minY, this.maxY) - this.minY;
        var relZ = MathHelper.clamp(blockZ, this.minZ, this.maxZ) - this.minZ;

        if (!this.slices.containsKey(resolver)) {
            this.slices.put(resolver, new Slice[this.sizeY]);
            var slice = this.slices.get(resolver);

            for (int y = 0; y < this.sizeY; y++) {
                slice[y] = new Slice(this.sizeXZ);
            }

            this.populatedSlices.put(resolver, new boolean[this.sizeY]);
            Arrays.fill(populatedSlices.get(resolver), false);
        }
        if (!this.populatedSlices.get(resolver)[relY]) {
            this.updateColorBuffers(relY, resolver);
        }

        var slice = this.slices.get(resolver)[relY];
        var buffer = slice.getBuffer();

        return buffer.get(relX, relZ);
    }

    private void updateColorBuffers(int relY, ColorResolver resolver) {
        var slice = this.slices.get(resolver)[relY];

        int worldY = this.minY + relY;

        for (int worldZ = this.minZ; worldZ <= this.maxZ; worldZ++) {
            for (int worldX = this.minX; worldX <= this.maxX; worldX++) {
                Biome biome = this.biomeData.getBiome(worldX, worldY, worldZ).value();

                int relativeX = worldX - this.minX;
                int relativeZ = worldZ - this.minZ;

                slice.buffer.set(relativeX, relativeZ, resolver.getColor(biome, worldX, worldZ));
            }
        }

        if (this.blendRadius > 0) {
            BoxBlur.blur(slice.buffer, this.tempColorBuffer, this.blendRadius);
        }

        this.populatedSlices.get(resolver)[relY] = true;
    }

    private static class Slice {
        private final ColorBuffer buffer;

        private Slice(int size) {
            this.buffer = new ColorBuffer(size, size);
        }

        public ColorBuffer getBuffer() {
            return this.buffer;
        }
    }
}
