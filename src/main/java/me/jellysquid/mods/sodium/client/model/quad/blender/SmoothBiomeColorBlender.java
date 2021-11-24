package me.jellysquid.mods.sodium.client.model.quad.blender;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadColorProvider;
import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import me.jellysquid.mods.sodium.client.util.color.ColorMixer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public class SmoothBiomeColorBlender implements BiomeColorBlender {
    private final int[] cachedRet = new int[4];

    private final BlockPos.Mutable mpos = new BlockPos.Mutable();

    @Override
    public <T> int[] getColors(BlockRenderView world, BlockPos origin, ModelQuadView quad, ModelQuadColorProvider<T> colorizer, T state) {
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

    private <T> int getVertexColor(ModelQuadColorProvider<T> colorizer, BlockRenderView world, T state, BlockPos origin,
                                   ModelQuadView quad, int vertexIdx) {
        final int x = origin.getX() + (int) quad.getX(vertexIdx);
        final int y = origin.getY() + (int) quad.getY(vertexIdx);
        final int z = origin.getZ() + (int) quad.getZ(vertexIdx);

        final int color = this.getBlockColor(colorizer, world, state, x, y, z, quad.getColorIndex());

        return ColorARGB.toABGR(color);
    }

    private <T> int getBlockColor(ModelQuadColorProvider<T> colorizer, BlockRenderView world, T state,
                                  int x, int y, int z, int colorIdx) {
        return colorizer.getColor(state, world, this.mpos.set(x, y, z), colorIdx);
    }

    private <T> int getInterpolatedVertexColor(ModelQuadColorProvider<T> colorizer, BlockRenderView world, T state,
                                               BlockPos origin, ModelQuadView quad, int vertexIdx) {
        final float x = quad.getX(vertexIdx);
        final float y = quad.getY(vertexIdx);
        final float z = quad.getZ(vertexIdx);

        final int intX = (int) x;
        final int intY = (int) y;
        final int intZ = (int) z;

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

        int c = ColorMixer.mixARGB(c0, c1, dx1, dx2);

        return ColorARGB.toABGR(c);
    }
}
