package me.jellysquid.mods.sodium.client.model.quad.blender;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.util.ColorMixer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockRenderView;
import java.util.Arrays;

public final class BiomeColorBlender {

    private final boolean useSmoothBlending;

    private final int[] cachedRet = new int[4];

    private final BlockPos.Mutable cachedPos = new BlockPos.Mutable();

    public BiomeColorBlender(int radius) {
        this.useSmoothBlending = radius > 0;
    }

    /**
     * Computes the per-vertex colors of a model quad.
     *
     * The array returned by this method may be re-used in subsequent calls in order to reduce memory allocations, and
     * as such, the contents of an array returned by this method are undefined after a subsequent call.
     *
     * @param world The world to sample biomes (and as a result, colors) from
     * @param origin The position of the block being rendered
     * @param quad The quad which will be colorized
     * @param sampler The source from which color will be sampled
     * @param state The block state being rendered
     * @return An array of ABGR colors
     */
    public <T> int[] getColors(BlockRenderView world, BlockPos origin, ModelQuadView quad, ColorSampler<T> sampler, T state) {
        final int[] colors = this.cachedRet;
        if (this.useSmoothBlending) {
            this.getColorsLinear(world, origin, quad, sampler, state, colors);
        } else {
            this.getColorsFlat(world, origin, quad, sampler, state, colors);
        }
        return colors;
    }

    private <T> void getColorsFlat(BlockRenderView world, BlockPos origin, ModelQuadView quad, ColorSampler<T> sampler, T state, int[] colors) {
        Arrays.fill(colors, ColorARGB.toABGR(sampler.getColor(state, world, origin, quad.getColorIndex())));
    }

    private <T> void getColorsLinear(BlockRenderView world, BlockPos origin, ModelQuadView quad, ColorSampler<T> sampler, T state, int[] colors) {
        for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
            colors[vertexIndex] = this.getVertexColor(world, origin, quad, sampler, state, vertexIndex);
        }
    }

    private <T> int getBlockColor(BlockRenderView world, T state, ColorSampler<T> sampler, int x, int y, int z, int colorIdx) {
        return sampler.getColor(state, world, this.cachedPos.set(x, y, z), colorIdx);
    }

    private <T> int getVertexColor(BlockRenderView world, BlockPos origin, ModelQuadView quad, ColorSampler<T> sampler, T state, int vertexIdx) {
        // Offset the position by -0.5f to align smooth blending with flat blending.
        final float posX = quad.getX(vertexIdx) - 0.5f;
        final float posY = quad.getY(vertexIdx) - 0.5f;
        final float posZ = quad.getZ(vertexIdx) - 0.5f;
        // Floor the positions here to always get the largest integer below the input
        // as negative values by default round toward zero when casting to an integer.
        // Which would cause negative ratios to be calculated in the interpolation later on.
        final int intX = MathHelper.floor(posX);
        final int intY = MathHelper.floor(posY);
        final int intZ = MathHelper.floor(posZ);
        // Integer component of position vector
        final int worldIntX = origin.getX() + intX;
        final int worldIntY = origin.getY() + intY;
        final int worldIntZ = origin.getZ() + intZ;
        // Retrieve the color values for each neighboring block
        final int c00 = this.getBlockColor(world, state, sampler, worldIntX + 0, worldIntY, worldIntZ + 0, quad.getColorIndex());
        final int c01 = this.getBlockColor(world, state, sampler, worldIntX + 0, worldIntY, worldIntZ + 1, quad.getColorIndex());
        final int c10 = this.getBlockColor(world, state, sampler, worldIntX + 1, worldIntY, worldIntZ + 0, quad.getColorIndex());
        final int c11 = this.getBlockColor(world, state, sampler, worldIntX + 1, worldIntY, worldIntZ + 1, quad.getColorIndex());
        // Linear interpolation across the Z-axis
        int z0;
        if (c00 != c01) {
            z0 = ColorMixer.mix(c00, c01, posZ - intZ);
        } else {
            z0 = c00;
        }
        int z1;
        if (c10 != c11) {
            z1 = ColorMixer.mix(c10, c11, posZ - intZ);
        } else {
            z1 = c10;
        }
        // Linear interpolation across the X-axis
        int x0;
        if (z0 != z1) {
            x0 = ColorMixer.mix(z0, z1, posX - intX);
        } else {
            x0 = z0;
        }
        return ColorARGB.toABGR(x0);
    }
}
