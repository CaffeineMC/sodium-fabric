package me.jellysquid.mods.sodium.client.render.light.cache;

import net.minecraft.world.BlockRenderView;

import java.util.Arrays;

public class ChunkLightDataCache extends LightDataCache {
    private final long[] light;
    private final int xSize, ySize, zSize;
    private int xOffset, yOffset, zOffset;

    public ChunkLightDataCache(int size) {
        this(size, size, size);
    }

    public ChunkLightDataCache(int xSize, int ySize, int zSize) {
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