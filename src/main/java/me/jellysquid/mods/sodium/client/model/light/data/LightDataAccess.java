package me.jellysquid.mods.sodium.client.model.light.data;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.LightType;

/**
 * The light data cache is used to make accessing the light data and occlusion properties of blocks cheaper. The data
 * for each block is stored as a long integer with packed fields in order to work around the lack of value types in Java.
 *
 * This code is not very pretty, but it does perform significantly faster than the vanilla implementation and has
 * good cache locality.
 *
 * Each long integer contains the following fields:
 * - BL: World block light, encoded as a 4-bit unsigned integer
 * - SL: World sky light, encoded as a 4-bit unsigned integer
 * - LU: Block luminance, encoded as a 4-bit unsigned integer
 * - AO: Ambient occlusion, floating point value in the range of 0.0..1.0 encoded as a 16-bit unsigned integer with 12-bit precision
 * - EM: Emissive test, true if block uses emissive lighting
 * - OP: Block opacity test, true if opaque
 * - FO: Full cube opacity test, true if opaque full cube
 * - FC: Full cube test, true if full cube
 *
 * You can use the various static pack/unpack methods to extract these values in a usable format.
 */
public abstract class LightDataAccess {
    private final BlockPos.Mutable pos = new BlockPos.Mutable();
    protected BlockRenderView world;

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

    /**
     * Returns the light data for the block at the given position. The property fields can then be accessed using
     * the various unpack methods below.
     */
    public abstract long get(int x, int y, int z);

    protected long compute(int x, int y, int z) {
        BlockPos pos = this.pos.set(x, y, z);
        BlockRenderView world = this.world;

        BlockState state = world.getBlockState(pos);

        boolean em = state.hasEmissiveLighting(world, pos);
        boolean op = state.shouldBlockVision(world, pos) && state.getOpacity(world, pos) != 0;
        boolean fo = state.isOpaqueFullCube(world, pos);
        boolean fc = state.isFullCube(world, pos);

        int lu = state.getLuminance();

        // OPTIMIZE: Do not calculate light data if the block is full and opaque and does not emit light.
        int bl;
        int sl;
        if (fo && lu == 0) {
            bl = 0;
            sl = 0;
        } else {
            bl = world.getLightLevel(LightType.BLOCK, pos);
            sl = world.getLightLevel(LightType.SKY, pos);
        }

        // FIX: Do not apply AO from blocks that emit light
        float ao;
        if (lu == 0) {
            ao = state.getAmbientOcclusionLightLevel(world, pos);
        } else {
            ao = 1.0f;
        }

        return packFC(fc) | packFO(fo) | packOP(op) | packEM(em) | packAO(ao) | packLU(lu) | packSL(sl) | packBL(bl);
    }

    public static long packBL(int blockLight) {
        return (long) blockLight & 0xFL;
    }

    public static int unpackBL(long word) {
        return (int) (word & 0xFL);
    }

    public static long packSL(int skyLight) {
        return ((long) skyLight & 0xFL) << 4;
    }

    public static int unpackSL(long word) {
        return (int) (word >> 4 & 0xFL);
    }

    public static long packLU(int luminance) {
        return ((long) luminance & 0xFL) << 8;
    }

    public static int unpackLU(long word) {
        return (int) (word >> 8 & 0xFL);
    }

    public static long packAO(float ao) {
        int aoi = (int) (ao * 4096.0f);
        return ((long) aoi & 0xFFFFL) << 12;
    }

    public static float unpackAO(long word) {
        int aoi = (int) (word >>> 12 & 0xFFFFL);
        return aoi * (1.0f / 4096.0f);
    }

    public static long packEM(boolean emissive) {
        return (emissive ? 1L : 0L) << 28;
    }

    public static boolean unpackEM(long word) {
        return ((word >>> 28) & 0b1) != 0;
    }

    public static long packOP(boolean opaque) {
        return (opaque ? 1L : 0L) << 29;
    }

    public static boolean unpackOP(long word) {
        return ((word >>> 29) & 0b1) != 0;
    }

    public static long packFO(boolean opaque) {
        return (opaque ? 1L : 0L) << 30;
    }

    public static boolean unpackFO(long word) {
        return ((word >>> 30) & 0b1) != 0;
    }

    public static long packFC(boolean fullCube) {
        return (fullCube ? 1L : 0L) << 31;
    }

    public static boolean unpackFC(long word) {
        return ((word >>> 31) & 0b1) != 0;
    }

    /**
     * Computes the combined lightmap using block light, sky light, and luminance values.
     *
     * <p>This method's logic is equivalent to
     * {@link WorldRenderer#getLightmapCoordinates(BlockRenderView, BlockPos)}, but without the
     * emissive check.
     */
    public static int getLightmap(long word) {
        return LightmapTextureManager.pack(Math.max(unpackBL(word), unpackLU(word)), unpackSL(word));
    }

    /**
     * Like {@link #getLightmap(long)}, but checks {@link #unpackEM(long)} first and returns
     * the {@link LightmapTextureManager#MAX_LIGHT_COORDINATE fullbright lightmap} if emissive.
     *
     * <p>This method's logic is equivalent to
     * {@link WorldRenderer#getLightmapCoordinates(BlockRenderView, BlockPos)}.
     */
    public static int getEmissiveLightmap(long word) {
        if (unpackEM(word)) {
            return LightmapTextureManager.MAX_LIGHT_COORDINATE;
        } else {
            return getLightmap(word);
        }
    }

    public BlockRenderView getWorld() {
        return this.world;
    }
}