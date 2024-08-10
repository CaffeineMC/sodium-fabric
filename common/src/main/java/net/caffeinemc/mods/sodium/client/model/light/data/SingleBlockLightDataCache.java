package net.caffeinemc.mods.sodium.client.model.light.data;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;

import java.util.Arrays;

/**
 * Adaptation of {@link ArrayLightDataCache} that stores data for
 * only one block and its neighbors.
 */
public class SingleBlockLightDataCache extends LightDataAccess {
    // Radius of 2 is required: block models may create geometry in up to a 3x3x3 region, which requires light to be
    // queried from neighbours of any block in this region
    private static final int NEIGHBOR_BLOCK_RADIUS = 2;
    private static final int BLOCK_LENGTH = 1 + (NEIGHBOR_BLOCK_RADIUS * 2);

    private final int[] light;

    private int xOffset, yOffset, zOffset;

    public SingleBlockLightDataCache() {
        this.light = new int[BLOCK_LENGTH * BLOCK_LENGTH * BLOCK_LENGTH];
    }

    public void reset(BlockPos origin, BlockAndTintGetter blockView) {
        this.xOffset = origin.getX() - NEIGHBOR_BLOCK_RADIUS;
        this.yOffset = origin.getY() - NEIGHBOR_BLOCK_RADIUS;
        this.zOffset = origin.getZ() - NEIGHBOR_BLOCK_RADIUS;

        Arrays.fill(this.light, 0);

        this.level = blockView;
    }

    public void release() {
        this.level = null;
    }

    private int index(int x, int y, int z) {
        int x2 = x - this.xOffset;
        int y2 = y - this.yOffset;
        int z2 = z - this.zOffset;

        return (z2 * BLOCK_LENGTH * BLOCK_LENGTH) + (y2 * BLOCK_LENGTH) + x2;
    }

    @Override
    public int get(int x, int y, int z) {
        int l = this.index(x, y, z);

        int word = this.light[l];

        if (word != 0) {
            return word;
        }

        return this.light[l] = this.compute(x, y, z);
    }
}