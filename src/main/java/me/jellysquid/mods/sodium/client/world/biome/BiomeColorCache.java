package me.jellysquid.mods.sodium.client.world.biome;

import me.jellysquid.mods.sodium.client.render.chunk.ColorResolverType;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.biome.BoxBlur.ColorBuffer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.ColorResolver;

public class BiomeColorCache {
    private final WorldSlice slice;

    private final CachedColorBuffers[] cachedColorBuffers;
    private final int worldX, worldZ;

    public final int radius;
    public final int inset;
    public final int size;

    public BiomeColorCache(WorldSlice slice, int radius) {
        this.slice = slice;

        this.radius = radius;

        int margin = MathHelper.clamp(this.radius, 3, 15);

        this.size = 16 + (margin * 2);
        this.inset = 16 - margin;

        this.cachedColorBuffers = new CachedColorBuffers[this.size];

        var origin = slice.getOrigin();

        this.worldX = origin.getMinX() - margin;
        this.worldZ = origin.getMinZ() - margin;
    }

    public int getColor(ColorResolver resolver, int blockX, int blockY, int blockZ) {
        var x = MathHelper.clamp(blockX - this.inset, 0, this.size - 1);
        var y = MathHelper.clamp(blockY - this.inset, 0, this.size - 1);
        var z = MathHelper.clamp(blockZ - this.inset, 0, this.size - 1);

        var buffers = this.cachedColorBuffers[y];

        if (buffers == null) {
            this.cachedColorBuffers[y] = (buffers = this.createColorBuffers(y));
        }

        return buffers.get(ColorResolverType.get(resolver), x, z);
    }

    private CachedColorBuffers createColorBuffers(int y) {
        ColorBuffer[] buffers = new ColorBuffer[ColorResolverType.COUNT];

        for (int i = 0; i < ColorResolverType.COUNT; i++) {
            buffers[i] = new ColorBuffer(this.size, this.size);
        }

        ColorBuffer mapGrass = buffers[ColorResolverType.GRASS.ordinal()];
        ColorBuffer mapFoliage = buffers[ColorResolverType.FOLIAGE.ordinal()];
        ColorBuffer mapWater = buffers[ColorResolverType.WATER.ordinal()];

        for (int x = 0; x < this.size; x++) {
            for (int z = 0; z < this.size; z++) {
                Biome biome = this.slice.getBiome(this.inset + x, this.inset + y, this.inset + z);

                var index = (z * this.size) + x;

                mapGrass.setARGB(index, biome.getGrassColorAt(this.worldX + x, this.worldZ + z));
                mapFoliage.setARGB(index, biome.getFoliageColor());
                mapWater.setARGB(index, biome.getWaterColor());
            }
        }

        if (this.radius > 0) {
            for (ColorBuffer buffer : buffers) {
                BoxBlur.blur(buffer, this.radius);
            }
        }

        return new CachedColorBuffers(buffers);
    }

    private static class CachedColorBuffers {
        private final ColorBuffer[] buffers;

        private CachedColorBuffers(ColorBuffer[] buffers) {
            this.buffers = buffers;
        }

        public int get(ColorResolverType type, int x, int z) {
            var buffer = this.buffers[type.ordinal()];
            return buffer.getARGB(buffer.getIndex(x, z));
        }
    }
}
