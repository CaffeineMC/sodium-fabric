package me.jellysquid.mods.sodium.client.render.model.quad.blender;

import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.util.ColorUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public class BilinearVertexColorBlender implements VertexColorBlender {
    private final int[] cachedRet = new int[4];
    private final BlockPos.Mutable mpos = new BlockPos.Mutable();

    @Override
    public int[] getColors(BlockColorProvider provider, BlockState state, BlockRenderView world, ModelQuadView quad, BlockPos origin, int colorIndex, float[] brightness) {
        final int[] colors = this.cachedRet;

        for (int i = 0; i < 4; i++) {
            colors[i] = this.getVertexColor(provider, state, world, quad.getX(i), quad.getZ(i), origin, quad.getColor(i), colorIndex, brightness[i]);
        }

        return colors;
    }

    private int getVertexColor(BlockColorProvider provider, BlockState state, BlockRenderView world, float posX, float posZ, BlockPos origin, int color, int colorIndex, float brightness) {
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
            fr = ColorUtil.unpackColorR(c1);
            fg = ColorUtil.unpackColorG(c1);
            fb = ColorUtil.unpackColorB(c1);
        } else {
            // RGB components for each corner's color
            final float c1r = ColorUtil.unpackColorR(c1);
            final float c1g = ColorUtil.unpackColorG(c1);
            final float c1b = ColorUtil.unpackColorB(c1);

            final float c2r = ColorUtil.unpackColorR(c2);
            final float c2g = ColorUtil.unpackColorG(c2);
            final float c2b = ColorUtil.unpackColorB(c2);

            final float c3r = ColorUtil.unpackColorR(c3);
            final float c3g = ColorUtil.unpackColorG(c3);
            final float c3b = ColorUtil.unpackColorB(c3);

            final float c4r = ColorUtil.unpackColorR(c4);
            final float c4g = ColorUtil.unpackColorG(c4);
            final float c4b = ColorUtil.unpackColorB(c4);

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
        return ColorUtil.mulPackedRGB(color,
                ColorUtil.normalize(fr) * brightness,
                ColorUtil.normalize(fg) * brightness,
                ColorUtil.normalize(fb) * brightness);
    }
}
