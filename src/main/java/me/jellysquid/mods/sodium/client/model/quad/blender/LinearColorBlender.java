package me.jellysquid.mods.sodium.client.model.quad.blender;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import me.jellysquid.mods.sodium.client.util.color.ColorMixer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockRenderView;

public class LinearColorBlender implements ColorBlender {
    private final int[] cachedRet = new int[4];

    private final BlockPos.Mutable mpos = new BlockPos.Mutable();

    @Override
    public <T> int[] getColors(BlockRenderView world, BlockPos origin, ModelQuadView quad, ColorSampler<T> sampler, T state) {
        final int[] colors = this.cachedRet;

        for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
            colors[vertexIndex] = this.getVertexColor(world, origin, quad, sampler, state, vertexIndex);
        }

        return colors;
    }

    private <T> int getBlockColor(BlockRenderView world, T state, ColorSampler<T> sampler,
                                  int x, int y, int z, int colorIdx) {
        return sampler.getColor(state, world, this.mpos.set(x, y, z), colorIdx);
    }

    private <T> int getVertexColor(BlockRenderView world, BlockPos origin, ModelQuadView quad, ColorSampler<T> sampler, T state,
                                   int vertexIdx) {
        // Clamp positions to the range -1.0f to +2.0f to prevent crashes with badly behaved
        // block models and mods causing out-of-bounds array accesses in BiomeColorCache.
        // Offset the position by -0.5f after clamping to align smooth blending with flat blending.
        final float x = MathHelper.clamp(quad.getX(vertexIdx), -1.0f, 2.0f) - 0.5f;
        final float y = MathHelper.clamp(quad.getY(vertexIdx), -1.0f, 2.0f) - 0.5f;
        final float z = MathHelper.clamp(quad.getZ(vertexIdx), -1.0f, 2.0f) - 0.5f;

        // Floor the positions here to always get the largest integer below the input
        // as negative values by default round toward zero when casting to an integer.
        // Which would cause negative ratios to be calculated in the interpolation later on.
        final int intX = (int) Math.floor(x);
        final int intY = (int) Math.floor(y);
        final int intZ = (int) Math.floor(z);

        // Integer component of position vector
        final int originX = origin.getX() + intX;
        final int originY = origin.getY() + intY;
        final int originZ = origin.getZ() + intZ;

        // Retrieve the color values for each neighboring block
        final int c00 = this.getBlockColor(world, state, sampler, originX, originY, originZ, quad.getColorIndex());
        final int c01 = this.getBlockColor(world, state, sampler, originX, originY, originZ + 1, quad.getColorIndex());
        final int c10 = this.getBlockColor(world, state, sampler, originX + 1, originY, originZ, quad.getColorIndex());
        final int c11 = this.getBlockColor(world, state, sampler, originX + 1, originY, originZ + 1, quad.getColorIndex());

        // Fraction component of position vector
        final float fracX = x - intX;
        final float fracZ = z - intZ;

        // Linear interpolation across the Z-axis
        int dz1 = ColorMixer.getStartRatio(fracZ);
        int dz2 = ColorMixer.getEndRatio(fracZ);
        int rz0 = ColorMixer.mix(c00, c01, dz1, dz2);
        int rz1 = ColorMixer.mix(c10, c11, dz1, dz2);

        // Linear interpolation across the X-axis
        int dx1 = ColorMixer.getStartRatio(fracX);
        int dx2 = ColorMixer.getEndRatio(fracX);
        int rx = ColorMixer.mix(rz0, rz1, dx1, dx2);

        return ColorARGB.toABGR(rx);
    }
}
