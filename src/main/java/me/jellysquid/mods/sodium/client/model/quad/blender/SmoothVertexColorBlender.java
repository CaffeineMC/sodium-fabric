package me.jellysquid.mods.sodium.client.model.quad.blender;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import me.jellysquid.mods.sodium.client.util.ColorRGBA;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public class SmoothVertexColorBlender implements VertexColorBlender {
    private final int[] cachedRet = new int[4];
    private final BlockPos.Mutable mpos = new BlockPos.Mutable();

    @Override
    public int[] getColors(BlockColorProvider provider, BlockState state, BlockRenderView world, ModelQuadView quad, BlockPos origin, int colorIndex, float[] brightness) {
        final int[] colors = this.cachedRet;

        for (int i = 0; i < 4; i++) {
            float x = quad.getX(i);
            float z = quad.getZ(i);

            // If the vertex is aligned to the block grid, we do not need to interpolate
            if (ModelQuadFlags.contains(quad.getFlags(), ModelQuadFlags.IS_ALIGNED)) {
                colors[i] = this.getVertexColor(provider, state, world, x, z, origin, quad.getColor(i), colorIndex, brightness[i]);
            } else {
                colors[i] = this.getInterpolatedVertexColor(provider, state, world, x, z, origin, quad.getColor(i), colorIndex, brightness[i]);
            }
        }

        return colors;
    }

    private int getVertexColor(BlockColorProvider provider, BlockState state, BlockRenderView world, float posX, float posZ, BlockPos origin, int color, int colorIndex, float brightness) {
        final float x = origin.getX() + posX;
        final float z = origin.getZ() + posZ;

        final int intX = (int) x;
        final int intZ = (int) z;

        final BlockPos.Mutable mpos = this.mpos;

        final int f = provider.getColor(state, world, mpos.set(intX, origin.getY(), intZ), colorIndex);

        final float fr = ColorRGBA.normalize(ColorRGBA.unpackRed(f)) * brightness;
        final float fg = ColorRGBA.normalize(ColorRGBA.unpackGreen(f)) * brightness;
        final float fb = ColorRGBA.normalize(ColorRGBA.unpackBlue(f)) * brightness;

        return ColorRGBA.mulPackABGR(color, fr, fg, fb);
    }

    private int getInterpolatedVertexColor(BlockColorProvider provider, BlockState state, BlockRenderView world, float posX, float posZ, BlockPos origin, int color, int colorIndex, float brightness) {
        final BlockPos.Mutable mpos = this.mpos;

        final float x = origin.getX() + posX;
        final float z = origin.getZ() + posZ;

        // Integer component of position vector
        final int intX = (int) x;
        final int intZ = (int) z;

        // Fraction component of position vector
        final float fracX = x - intX;
        final float fracZ = z - intZ;

        // Retrieve the color values for each neighbor
        final int c1 = provider.getColor(state, world, mpos.set(intX, origin.getY(), intZ), colorIndex);
        final int c2 = provider.getColor(state, world, mpos.set(intX, origin.getY(), intZ + 1), colorIndex);
        final int c3 = provider.getColor(state, world, mpos.set(intX + 1, origin.getY(), intZ), colorIndex);
        final int c4 = provider.getColor(state, world, mpos.set(intX + 1, origin.getY(), intZ + 1), colorIndex);

        final float fr, fg, fb;

        // All the colors are the same, so the results of interpolation will be useless.
        if (c1 == c2 && c2 == c3 && c3 == c4) {
            fr = ColorRGBA.unpackRed(c1);
            fg = ColorRGBA.unpackGreen(c1);
            fb = ColorRGBA.unpackBlue(c1);
        } else {
            // TODO: avoid float conversions here
            // RGB components for each corner's color
            final float c1r = ColorRGBA.unpackRed(c1);
            final float c1g = ColorRGBA.unpackGreen(c1);
            final float c1b = ColorRGBA.unpackBlue(c1);

            final float c2r = ColorRGBA.unpackRed(c2);
            final float c2g = ColorRGBA.unpackGreen(c2);
            final float c2b = ColorRGBA.unpackBlue(c2);

            final float c3r = ColorRGBA.unpackRed(c3);
            final float c3g = ColorRGBA.unpackGreen(c3);
            final float c3b = ColorRGBA.unpackBlue(c3);

            final float c4r = ColorRGBA.unpackRed(c4);
            final float c4g = ColorRGBA.unpackGreen(c4);
            final float c4b = ColorRGBA.unpackBlue(c4);

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

        // Normalize and darken the returned color
        return ColorRGBA.mulPackABGR(color,
                ColorRGBA.normalize(fr) * brightness,
                ColorRGBA.normalize(fg) * brightness,
                ColorRGBA.normalize(fb) * brightness);
    }
}
