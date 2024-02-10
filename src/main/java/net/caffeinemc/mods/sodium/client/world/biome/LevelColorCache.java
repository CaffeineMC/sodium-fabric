package net.caffeinemc.mods.sodium.client.world.biome;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.caffeinemc.mods.sodium.client.util.color.BoxBlur;
import net.caffeinemc.mods.sodium.client.util.color.BoxBlur.ColorBuffer;
import net.caffeinemc.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;

/**
 * A slice of the level's color data.
 *
 * <p>This implementation varies significantly from Minecraft in the following ways:</p>
 *
 * <ul>
 *     <li>The color values for each {@link ColorResolver} are queried at the same time, so that
 *     the biome does not need to be fetched multiple times (very expensive).</li>
 *
 *     <li>Blending is performed against an entire array of color values at once, reducing the
 *     time complexity from O(K(R^2)) to O(2(R^2)), where K= the number of queries, and R= the blend radius.</li>
 * </ul>
 */
public class LevelColorCache {
    /**
     * The number of blocks around the origin chunk which should also have color data. This is necessary since block
     * models can extend outside the block cell, and as such, can also extend outside the chunk being rendered.
     */
    private static final int NEIGHBOR_BLOCK_RADIUS = 2;
    private final LevelBiomeSlice biomeData;

    /**
     * A cached lookup-table of blended color slices for each {@link ColorResolver}.
     */
    private final Reference2ReferenceOpenHashMap<ColorResolver, Slice[]> slices;

    /**
     * A timestamp which is incremented each time the {@link LevelBiomeSlice} is changed. This avoids the need to reset
     * the cached arrays, since querying a cached array will check this timestamp and only reset it if necessary.
     */
    private long populateStamp;

    /**
     * The radius (in blocks) to blend colors.
     */
    private final int blendRadius;

    /**
     * The temporary buffer used while blending colors.
     */
    private final ColorBuffer tempColorBuffer;

    private int minBlockX, minBlockY, minBlockZ;
    private int maxBlockX, maxBlockY, maxBlockZ;

    /**
     * The size of the cached lookup table in the horizontal and vertical directions. These are separate since blending
     * is only done across the horizontal access, and we don't want to allocate memory for vertical slices that will
     * never be used.
     */
    private final int sizeXZ, sizeY;

    public LevelColorCache(LevelBiomeSlice biomeData, int blendRadius) {
        this.biomeData = biomeData;
        this.blendRadius = blendRadius;

        this.sizeXZ = 16 + ((NEIGHBOR_BLOCK_RADIUS + this.blendRadius) * 2);
        this.sizeY = 16 + (NEIGHBOR_BLOCK_RADIUS * 2);

        this.slices = new Reference2ReferenceOpenHashMap<>();
        this.populateStamp = 1;

        this.tempColorBuffer = new ColorBuffer(this.sizeXZ, this.sizeXZ);
    }

    public void update(ChunkRenderContext context) {
        this.minBlockX = (context.getOrigin().minBlockX() - NEIGHBOR_BLOCK_RADIUS) - this.blendRadius;
        this.minBlockY = (context.getOrigin().minBlockY() - NEIGHBOR_BLOCK_RADIUS);
        this.minBlockZ = (context.getOrigin().minBlockZ() - NEIGHBOR_BLOCK_RADIUS) - this.blendRadius;

        this.maxBlockX = (context.getOrigin().maxBlockX() + NEIGHBOR_BLOCK_RADIUS) + this.blendRadius;
        this.maxBlockY = (context.getOrigin().maxBlockY() + NEIGHBOR_BLOCK_RADIUS);
        this.maxBlockZ = (context.getOrigin().maxBlockZ() + NEIGHBOR_BLOCK_RADIUS) + this.blendRadius;

        this.populateStamp++;
    }

    public int getColor(BiomeColorSource source, int blockX, int blockY, int blockZ) {
        return switch (source) {
            case GRASS -> getColor(BiomeColors.GRASS_COLOR_RESOLVER, blockX, blockY, blockZ);
            case FOLIAGE -> getColor(BiomeColors.FOLIAGE_COLOR_RESOLVER, blockX, blockY, blockZ);
            case WATER -> getColor(BiomeColors.WATER_COLOR_RESOLVER, blockX, blockY, blockZ);
        };
    }

    public int getColor(ColorResolver resolver, int blockX, int blockY, int blockZ) {
        var relBlockX = Mth.clamp(blockX, this.minBlockX, this.maxBlockX) - this.minBlockX;
        var relBlockY = Mth.clamp(blockY, this.minBlockY, this.maxBlockY) - this.minBlockY;
        var relBlockZ = Mth.clamp(blockZ, this.minBlockZ, this.maxBlockZ) - this.minBlockZ;

        if (!this.slices.containsKey(resolver)) {
            this.initializeSlices(resolver);
        }

        var slice = this.slices.get(resolver)[relBlockY];

        if (slice.lastPopulateStamp < this.populateStamp) {
            this.updateColorBuffers(relBlockY, resolver, slice);
        }

        var buffer = slice.getBuffer();

        return buffer.get(relBlockX, relBlockZ);
    }

    private void initializeSlices(ColorResolver resolver) {
        var slice = new Slice[this.sizeY];

        for (int blockY = 0; blockY < this.sizeY; blockY++) {
            slice[blockY] = new Slice(this.sizeXZ);
        }

        this.slices.put(resolver, slice);
    }

    private void updateColorBuffers(int relY, ColorResolver resolver, Slice slice) {
        int blockY = this.minBlockY + relY;

        for (int blockZ = this.minBlockZ; blockZ <= this.maxBlockZ; blockZ++) {
            for (int blockX = this.minBlockX; blockX <= this.maxBlockX; blockX++) {
                Biome biome = this.biomeData.getBiome(blockX, blockY, blockZ).value();

                int relBlockX = blockX - this.minBlockX;
                int relBlockZ = blockZ - this.minBlockZ;

                slice.buffer.set(relBlockX, relBlockZ, resolver.getColor(biome, blockX, blockZ));
            }
        }

        if (this.blendRadius > 0) {
            BoxBlur.blur(slice.buffer, this.tempColorBuffer, this.blendRadius);
        }

        slice.lastPopulateStamp = this.populateStamp;
    }

    private static class Slice {
        private final ColorBuffer buffer;
        private long lastPopulateStamp;

        private Slice(int size) {
            this.buffer = new ColorBuffer(size, size);
            this.lastPopulateStamp = 0;
        }

        public ColorBuffer getBuffer() {
            return this.buffer;
        }
    }
}
