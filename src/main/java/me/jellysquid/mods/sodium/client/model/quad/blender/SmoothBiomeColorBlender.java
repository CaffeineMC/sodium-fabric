package me.jellysquid.mods.sodium.client.model.quad.blender;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import me.jellysquid.mods.sodium.client.util.color.ColorU8;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public class SmoothBiomeColorBlender implements BiomeColorBlender {
    private final int[] cachedRet = new int[4];

    private final BlockPos.Mutable mpos = new BlockPos.Mutable();

    @Override
    public int[] getColors(BlockColorProvider colorizer, BlockRenderView world, BlockState state, BlockPos origin,
                           ModelQuadView quad) {
        final int[] colors = this.cachedRet;

        boolean aligned = ModelQuadFlags.contains(quad.getFlags(), ModelQuadFlags.IS_ALIGNED);

        for (int i = 0; i < 4; i++) {
            // If the vertex is aligned to the block grid, we do not need to interpolate
            if (aligned) {
                colors[i] = this.getVertexColor(colorizer, world, state, origin, quad, i);
            } else {
                colors[i] = this.getInterpolatedVertexColor(colorizer, world, state, origin, quad, i);
            }
        }

        return colors;
    }

    private int getVertexColor(BlockColorProvider colorizer, BlockRenderView world, BlockState state, BlockPos origin,
                               ModelQuadView quad, int vertexIdx) {
        final int x = (int) (origin.getX() + quad.getX(vertexIdx));
        final int z = (int) (origin.getZ() + quad.getZ(vertexIdx));

        final int color = this.getBlockColor(colorizer, world, state, origin, x, z, quad.getColorIndex());

        return ColorARGB.toABGR(color);
    }

    private int getBlockColor(BlockColorProvider colorizer, BlockRenderView world, BlockState state, BlockPos origin,
                              int x, int z, int colorIdx) {
        return colorizer.getColor(state, world, this.mpos.set(x, origin.getY(), z), colorIdx);
    }

    private int getInterpolatedVertexColor(BlockColorProvider colorizer, BlockRenderView world, BlockState state,
                                           BlockPos origin, ModelQuadView quad, int vertexIdx) {
        final float x = origin.getX() + quad.getX(vertexIdx);
        final float z = origin.getZ() + quad.getZ(vertexIdx);

        // Integer component of position vector
        final int intX = (int) x;
        final int intZ = (int) z;

        // Fraction component of position vector
        final float fracX = x - intX;
        final float fracZ = z - intZ;

        // Retrieve the color values for each neighbor
        final int c1 = this.getBlockColor(colorizer, world, state, origin, intX, intZ, quad.getColorIndex());
        final int c2 = this.getBlockColor(colorizer, world, state, origin, intX, intZ + 1, quad.getColorIndex());
        final int c3 = this.getBlockColor(colorizer, world, state, origin, intX + 1, intZ, quad.getColorIndex());
        final int c4 = this.getBlockColor(colorizer, world, state, origin, intX + 1, intZ + 1, quad.getColorIndex());

        final float fr, fg, fb;

        // All the colors are the same, so the results of interpolation will be useless.
        if (c1 == c2 && c2 == c3 && c3 == c4) {
            fr = ColorARGB.unpackRed(c1);
            fg = ColorARGB.unpackGreen(c1);
            fb = ColorARGB.unpackBlue(c1);
        } else {
            // TODO: avoid float conversions here
            // RGB components for each corner's color
            final float c1r = ColorARGB.unpackRed(c1);
            final float c1g = ColorARGB.unpackGreen(c1);
            final float c1b = ColorARGB.unpackBlue(c1);

            final float c2r = ColorARGB.unpackRed(c2);
            final float c2g = ColorARGB.unpackGreen(c2);
            final float c2b = ColorARGB.unpackBlue(c2);

            final float c3r = ColorARGB.unpackRed(c3);
            final float c3g = ColorARGB.unpackGreen(c3);
            final float c3b = ColorARGB.unpackBlue(c3);

            final float c4r = ColorARGB.unpackRed(c4);
            final float c4g = ColorARGB.unpackGreen(c4);
            final float c4b = ColorARGB.unpackBlue(c4);

            // Compute the final color values across the Z axis
            final float r1r = c1r + ((c2r - c1r) * fracZ);
            final float r1g = c1g + ((c2g - c1g) * fracZ);
            final float r1b = c1b + ((c2b - c1b) * fracZ);

            final float r2r = c3r + ((c4r - c3r) * fracZ);
            final float r2g = c3g + ((c4g - c3g) * fracZ);
            final float r2b = c3b + ((c4b - c3b) * fracZ);

            // Compute the final color values across the X axis
            fr = r1r + ((r2r - r1r) * fracX);
            fg = r1g + ((r2g - r1g) * fracX);
            fb = r1b + ((r2b - r1b) * fracX);
        }

        return ColorABGR.pack(ColorU8.normalize(fr), ColorU8.normalize(fg), ColorU8.normalize(fb));
    }
}
