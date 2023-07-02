package me.jellysquid.mods.sodium.client.world.biome;

import me.jellysquid.mods.sodium.client.util.color.BoxBlur;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.util.color.BoxBlur.ColorBuffer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;

public class BiomeColorCache {
    private final WorldSlice slice;
    private final int originX, originZ;
    private final int minCoord, maxCoord;
    private final ColorBuffer[][] cache;
    private final int blendDistance;
    private final int length;

    public BiomeColorCache(WorldSlice slice, int blendDistance) {
        this.slice = slice;

        this.blendDistance = blendDistance;

        int margin = MathHelper.clamp(this.blendDistance, 3, 15);

        this.length = 16 + (margin * 2);

        this.minCoord = 16 - margin;
        this.maxCoord = this.length - 1;

        this.cache = new ColorBuffer[this.length][];

        var origin = slice.getOrigin();

        this.originX = origin.getMinX() - margin;
        this.originZ = origin.getMinZ() - margin;
    }

    public int getColor(BiomeColorSource source, int blockX, int blockY, int blockZ) {
        var relX = MathHelper.clamp(blockX - this.minCoord, 0, this.maxCoord);
        var relY = MathHelper.clamp(blockY - this.minCoord, 0, this.maxCoord);
        var relZ = MathHelper.clamp(blockZ - this.minCoord, 0, this.maxCoord);

        var buffers = this.cache[relY];

        if (buffers == null) {
            this.cache[relY] = (buffers = this.createColorBuffers(relY));
        }

        return buffers[source.ordinal()]
                .getARGB(relX, relZ);
    }

    private ColorBuffer[] createColorBuffers(int y) {
        ColorBuffer[] buffers = new ColorBuffer[BiomeColorSource.COUNT];

        for (int i = 0; i < BiomeColorSource.COUNT; i++) {
            buffers[i] = new ColorBuffer(this.length, this.length);
        }

        ColorBuffer bufGrass = buffers[BiomeColorSource.GRASS.ordinal()];
        ColorBuffer bufFoliage = buffers[BiomeColorSource.FOLIAGE.ordinal()];
        ColorBuffer bufWater = buffers[BiomeColorSource.WATER.ordinal()];

        for (int z = 0; z < this.length; z++) {
            for (int x = 0; x < this.length; x++) {
                Biome biome = this.slice.getBiome(this.minCoord + x, this.minCoord + y, this.minCoord + z);

                int index = (z * this.length) + x;

                bufGrass.setARGB(index, biome.getGrassColorAt(this.originX + x, this.originZ + z));
                bufFoliage.setARGB(index, biome.getFoliageColor());
                bufWater.setARGB(index, biome.getWaterColor());
            }
        }


        if (this.blendDistance > 0) {
            for (ColorBuffer buffer : buffers) {
                BoxBlur.blur(buffer, this.blendDistance);
            }
        }

        return buffers;
    }
}
