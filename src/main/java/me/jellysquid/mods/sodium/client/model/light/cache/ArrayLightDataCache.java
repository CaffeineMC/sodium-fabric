package me.jellysquid.mods.sodium.client.model.light.cache;

import me.jellysquid.mods.sodium.client.model.light.data.LightDataAccess;
import net.minecraft.world.BlockRenderView;

import java.util.Arrays;

/**
 * A light data cache which uses a flat-array to store the light data for the blocks in a given chunk and its direct
 * neighbors. This is considerably faster than using a hash table to lookup values for a given block position and
 * can be re-used by {@link me.jellysquid.mods.sodium.client.world.WorldSlice} to avoid allocations.
 */
public class ArrayLightDataCache extends LightDataAccess {
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

        final int size = xSize * ySize * zSize;

        this.light = new long[size];
    }

    public void init(BlockRenderView world, int x, int y, int z) {
        this.world = world;
        this.xOffset = x;
        this.yOffset = y;
        this.zOffset = z;

        Arrays.fill(this.light, 0L);
    }

    private int index(int x, int y, int z) {
        return (z - this.zOffset) * this.xSize * this.ySize + (y - this.yOffset) * this.zSize + x - this.xOffset;
    }

    @Override
    public long get(int x, int y, int z) {
        final int index = this.index(x, y, z);
        final long word = this.light[index];
        final boolean wordIsZero = word == 0L;

        return wordIsZero ? (this.light[index] = this.compute(x, y, z)) : word;
    }

}