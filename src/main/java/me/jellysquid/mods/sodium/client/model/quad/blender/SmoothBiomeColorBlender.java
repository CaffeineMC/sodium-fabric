package me.jellysquid.mods.sodium.client.model.quad.blender;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadColorProvider;
import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import me.jellysquid.mods.sodium.client.util.color.ColorMixer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockRenderView;

public class SmoothBiomeColorBlender implements BiomeColorBlender {
    private final int[] cachedRet = new int[4];

    private final BlockPos.Mutable mpos = new BlockPos.Mutable();

    @Override
    public <T> int[] getColors(BlockRenderView world, BlockPos origin, ModelQuadView quad, ModelQuadColorProvider<T> colorizer, T state) {
        final int[] colors = this.cachedRet;

        for (int i = 0; i < 4; i++) {
            colors[i] = this.getInterpolatedVertexColor(colorizer, world, state, origin, quad, i);
        }

        return colors;
    }

    private <T> int getVertexColor(ModelQuadColorProvider<T> colorizer, BlockRenderView world, T state, BlockPos origin,
                                   ModelQuadView quad, int vertexIdx) {
        final int x = origin.getX() + (int) quad.getX(vertexIdx);
        final int y = origin.getY() + (int) quad.getY(vertexIdx);
        final int z = origin.getZ() + (int) quad.getZ(vertexIdx);

        return this.getBlockColor(colorizer, world, state, x, y, z, quad.getColorIndex());
    }

    private <T> int getBlockColor(ModelQuadColorProvider<T> colorizer, BlockRenderView world, T state,
                                  int x, int y, int z, int colorIdx) {
        return colorizer.getColor(state, world, this.mpos.set(x, y, z), colorIdx);
    }

    private <T> int getInterpolatedVertexColor(ModelQuadColorProvider<T> colorizer, BlockRenderView world, T state,
                                               BlockPos origin, ModelQuadView quad, int vertexIdx) {
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

        // Retrieve the color values for each neighbor
        final int c000 = this.getBlockColor(colorizer, world, state, originX, originY, originZ, quad.getColorIndex());
        final int c001 = this.getBlockColor(colorizer, world, state, originX, originY, originZ + 1, quad.getColorIndex());
        final int c100 = this.getBlockColor(colorizer, world, state, originX + 1, originY, originZ, quad.getColorIndex());
        final int c101 = this.getBlockColor(colorizer, world, state, originX + 1, originY, originZ + 1, quad.getColorIndex());

        final int c010 = this.getBlockColor(colorizer, world, state, originX, originY + 1, originZ, quad.getColorIndex());
        final int c011 = this.getBlockColor(colorizer, world, state, originX, originY + 1, originZ + 1, quad.getColorIndex());
        final int c110 = this.getBlockColor(colorizer, world, state, originX + 1, originY + 1, originZ, quad.getColorIndex());
        final int c111 = this.getBlockColor(colorizer, world, state, originX + 1, originY + 1, originZ + 1, quad.getColorIndex());

        // Fraction component of position vector
        final float fracX = x - intX;
        final float fracY = y - intY;
        final float fracZ = z - intZ;

        int dx1 = ColorMixer.getStartRatio(fracX);
        int dx2 = ColorMixer.getEndRatio(fracX);

        int dy1 = ColorMixer.getStartRatio(fracY);
        int dy2 = ColorMixer.getEndRatio(fracY);

        int dz1 = ColorMixer.getStartRatio(fracZ);
        int dz2 = ColorMixer.getEndRatio(fracZ);

        int c00 = ColorMixer.mixARGB(c000, c001, dz1, dz2);
        int c01 = ColorMixer.mixARGB(c100, c101, dz1, dz2);
        int c10 = ColorMixer.mixARGB(c010, c011, dz1, dz2);
        int c11 = ColorMixer.mixARGB(c110, c111, dz1, dz2);

        int c0 = ColorMixer.mixARGB(c00, c01, dy1, dy2);
        int c1 = ColorMixer.mixARGB(c10, c11, dy1, dy2);

        return ColorMixer.mixARGB(c0, c1, dx1, dx2);
    }
}
