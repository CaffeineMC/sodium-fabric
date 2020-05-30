package me.jellysquid.mods.sodium.client.render.light.cache;

import net.minecraft.world.BlockRenderView;

import java.util.Arrays;

/**
 * A light data cache which uses a flat-array to store the light data for the blocks in a given chunk and its direct
 * neighbors. This is considerably faster than using a hash table to lookup values for a given block position and
 * can be re-used by {@link me.jellysquid.mods.sodium.client.world.WorldSlice} to avoid allocations.
 */
public class ArrayLightDataCache extends LightDataCache {
    private final long[] light;
    private final int xSize, ySize, zSize;
    private int xOffset, yOffset, zOffset;

    public ArrayLightDataCache(int size) {
        this(size, size, size);
    }

    public ArrayLightDataCache(int xSize, int ySize, int zSize) {
        this.xSize = xSize;
        this.ySize = ySize;
        this.zSize = zSize;

        int len = xSize * ySize * zSize;

        this.light = new long[len];
    }

    public void init(BlockRenderView world, int x, int y, int z) {
        this.world = world;

        this.xOffset = x;
        this.yOffset = y;
        this.zOffset = z;

        Arrays.fill(this.light, 0L);
    }

    private int index(int x, int y, int z) {
        // TODO: simplify
        return (z - this.zOffset) * this.xSize * this.ySize + (y - this.yOffset) * this.zSize + x - this.xOffset;
    }

    @Override
    public long get(int x, int y, int z) {
        int l = this.index(x, y, z);

        long word = this.light[l];

        if (word != 0) {
            return word;
        }

        return this.light[l] = this.compute(x, y, z);
    }

}