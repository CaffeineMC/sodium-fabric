package me.jellysquid.mods.sodium.client.render;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

import java.util.Arrays;

public class LightDataCache {
    private static final int RADIUS = 20;

    private final BlockRenderView world;
    private final int xOffset, yOffset, zOffset;

    private final long[] light = new long[RADIUS * RADIUS * RADIUS];

    private final BlockPos.Mutable pos = new BlockPos.Mutable();

    public LightDataCache(BlockRenderView world, int xOffset, int yOffset, int zOffset) {
        this.world = world;

        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.zOffset = zOffset;
    }

    private int index(int x, int y, int z) {
        return (z - this.zOffset) * RADIUS * RADIUS + (y - this.yOffset) * RADIUS + x - this.xOffset;
    }

    public long get(int x, int y, int z, Direction d1, Direction d2) {
        return this.get(x + d1.getOffsetX() + d2.getOffsetX(),
                y + d1.getOffsetY() + d2.getOffsetY(),
                z + d1.getOffsetZ() + d2.getOffsetZ());
    }

    public long get(int x, int y, int z, Direction dir) {
        return this.get(x + dir.getOffsetX(),
                y + dir.getOffsetY(),
                z + dir.getOffsetZ());
    }

    public long get(BlockPos pos, Direction dir) {
        return this.get(pos.getX(), pos.getY(), pos.getZ(), dir);
    }

    public long get(BlockPos pos) {
        return this.get(pos.getX(), pos.getY(), pos.getZ());
    }

    public long get(int x, int y, int z) {
        int l = this.index(x, y, z);

        long word = this.light[l];

        if (word != 0) {
            return word;
        }

        return this.light[l] = this.compute(x, y, z);
    }

    private long compute(int x, int y, int z) {
        this.pos.set(x, y, z);

        BlockState state = this.world.getBlockState(this.pos);

        float ao;

        if (state.getLuminance() == 0) {
            ao = state.getAmbientOcclusionLightLevel(this.world, this.pos);
        } else {
            ao = 1.0f;
        }

        int lm = WorldRenderer.getLightmapCoordinates(this.world, state, this.pos);
        boolean op = state.getOpacity(this.world, this.pos) == 0;
        boolean fo = state.isFullOpaque(this.world, this.pos);

        return packAO(ao) | packLM(lm) | packOP(op) | packFO(fo) | (1L << 60);
    }

    private static long packOP(boolean opaque) {
        return (opaque ? 1L : 0L) << 56;
    }

    public static boolean unpackOP(long word) {
        return ((word >>> 56) & 0b1) != 0;
    }

    private static long packFO(boolean opaque) {
        return (opaque ? 1L : 0L) << 57;
    }

    public static boolean unpackFO(long word) {
        return ((word >>> 57) & 0b1) != 0;
    }

    public static long packAO(float ao) {
        int aoi = (int) (ao * 4096.0f);
        return ((long) aoi & 0xFFFFL) << 32;
    }

    public static long packLM(int lm) {
        return (long) lm & 0xFFFFFFFFL;
    }

    public static float unpackAO(long word) {
        int aoi = (int) (word >>> 32 & 0xFFFFL);
        return aoi / 4096.0f;
    }

    public static int unpackLM(long word) {
        return (int) (word & 0xFFFFFFFFL);
    }
}