package me.jellysquid.mods.sodium.client.render;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

import java.util.Arrays;

public class LightDataCache {
    private static final FluidState EMPTY_FLUID_STATE = Fluids.EMPTY.getDefaultState();

    private final byte[] op;
    private final long[] light;
    private final BlockPos.Mutable pos = new BlockPos.Mutable();

    private final int xSize, ySize, zSize;

    private BlockRenderView world;
    private int xOffset, yOffset, zOffset;

    public LightDataCache(int size) {
        this(size, size, size);
    }

    public LightDataCache(int xSize, int ySize, int zSize) {
        this.xSize = xSize;
        this.ySize = ySize;
        this.zSize = zSize;

        int len = xSize * ySize * zSize;

        this.light = new long[len];
        this.op = new byte[len];
    }

    public void init(BlockRenderView world, int x, int y, int z) {
        this.world = world;
        this.xOffset = x;
        this.yOffset = y;
        this.zOffset = z;

        Arrays.fill(this.light, 0L);
        Arrays.fill(this.op, (byte) 0);
    }

    private int index(int x, int y, int z) {
        return (z - this.zOffset) * this.xSize * this.ySize + (y - this.yOffset) * this.zSize + x - this.xOffset;
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
        BlockPos pos = this.pos.set(x, y, z);
        BlockRenderView world = this.world;

        BlockState state = world.getBlockState(pos);

        float ao;

        if (state.getLuminance() == 0) {
            ao = state.getAmbientOcclusionLightLevel(world, pos);
        } else {
            ao = 1.0f;
        }

        // FIX: Fluids are always non-translucent despite blocking light, so we need a special check here in order to
        // solve lighting issues underwater.
        boolean op = state.getFluidState() != EMPTY_FLUID_STATE || state.getOpacity(world, pos) == 0;
        boolean fo = state.isFullOpaque(world, pos);

        // OPTIMIZE: Do not calculate lightmap data if the block is full and opaque
        int lm = fo ? 0 : WorldRenderer.getLightmapCoordinates(world, state, pos);

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