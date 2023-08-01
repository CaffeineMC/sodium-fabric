package me.jellysquid.mods.sodium.client.world.biome;

import me.jellysquid.mods.sodium.client.util.color.BoxBlur;
import me.jellysquid.mods.sodium.client.util.color.BoxBlur.ColorBuffer;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;

import java.util.Arrays;
import java.util.Objects;

public class BiomeColorCache {
    private final int minCoord, maxCoord;
    private final Slice[] slices;
    private final boolean[] populatedSlices;

    private final int blendDistance;
    private final int diameter;
    private final int margin;

    private final ColorBuffer tempColorBuffer;

    private BiomeCache biomeCache;
    private int originX, originZ;

    public BiomeColorCache(int blendDistance) {
        this.blendDistance = blendDistance;

        this.margin = MathHelper.clamp(this.blendDistance, 3, 15);

        this.diameter = 16 + (this.margin * 2);

        this.minCoord = 16 - this.margin;
        this.maxCoord = this.diameter - 1;

        this.slices = new Slice[this.diameter];
        this.populatedSlices = new boolean[this.diameter];

        for (int y = 0; y < this.diameter; y++) {
            this.slices[y] = new Slice(this.diameter);
        }

        this.tempColorBuffer = new ColorBuffer(this.diameter, this.diameter);
    }

    public void update(WorldSlice slice) {
        this.biomeCache = slice.getBiomeCache();

        this.originX = slice.getOrigin().getMinX() - this.margin;
        this.originZ = slice.getOrigin().getMinZ() - this.margin;

        Arrays.fill(this.populatedSlices, false);
    }

    public int getColor(BiomeColorSource source, int blockX, int blockY, int blockZ) {
        var relX = MathHelper.clamp(blockX - this.minCoord, 0, this.maxCoord);
        var relY = MathHelper.clamp(blockY - this.minCoord, 0, this.maxCoord);
        var relZ = MathHelper.clamp(blockZ - this.minCoord, 0, this.maxCoord);

        if (!this.populatedSlices[relY]) {
            this.updateColorBuffers(relY);
        }

        var slice = this.slices[relY];
        var buffer = slice.getBuffer(source);

        return buffer.get(relX, relZ);
    }

    private void updateColorBuffers(int y) {
        var slice = this.slices[y];

        for (int z = 0; z < this.diameter; z++) {
            for (int x = 0; x < this.diameter; x++) {
                Biome biome = this.biomeCache.getBiome(this.minCoord + x, this.minCoord + y, this.minCoord + z);

                slice.grass.set(x, z, biome.getGrassColorAt(this.originX + x, this.originZ + z));
                slice.foliage.set(x, z, biome.getFoliageColor());
                slice.water.set(x, z, biome.getWaterColor());
            }
        }

        if (this.blendDistance > 0) {
            BoxBlur.blur(slice.grass, this.tempColorBuffer, this.blendDistance);
            BoxBlur.blur(slice.foliage, this.tempColorBuffer, this.blendDistance);
            BoxBlur.blur(slice.water, this.tempColorBuffer, this.blendDistance);
        }

        this.populatedSlices[y] = true;
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
