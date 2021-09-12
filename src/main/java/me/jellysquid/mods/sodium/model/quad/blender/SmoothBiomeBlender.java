package me.jellysquid.mods.sodium.model.quad.blender;

import me.jellysquid.mods.sodium.interop.fabric.helper.GeometryHelper;
import me.jellysquid.mods.sodium.interop.fabric.mesh.QuadViewImpl;
import me.jellysquid.mods.sodium.model.quad.QuadColorizer;
import me.jellysquid.mods.sodium.util.color.ColorMixer;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.state.State;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

class SmoothBiomeBlender implements BiomeBlender {
    private final BlockPos.Mutable mpos = new BlockPos.Mutable();

    @Override
    public <T extends State<O, ?>, O> void getColors(BlockRenderView world, T state, BlockPos origin, QuadView quad, QuadColorizer<T> resolver, int[] colors) {
        boolean aligned = (((QuadViewImpl) quad).geometryFlags() & GeometryHelper.AXIS_ALIGNED_FLAG) != 0;

        for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
            // If the vertex is aligned to the block grid, we do not need to interpolate
            if (aligned) {
                colors[vertexIndex] = this.getVertexColor(world, origin, state, quad, vertexIndex, resolver);
            } else {
                colors[vertexIndex] = this.getInterpolatedVertexColor(world, origin, state, quad, vertexIndex, resolver);
            }
        }
    }

    private <T> int getVertexColor(BlockRenderView world, BlockPos origin, T state,
                                   QuadView quad, int vertexIdx, QuadColorizer<T> resolver) {
        final int x = origin.getX() + (int) quad.x(vertexIdx);
        final int z = origin.getZ() + (int) quad.z(vertexIdx);

        return this.getBlockColor(world, origin, state, x, z, quad, resolver);
    }

    private <T> int getBlockColor(BlockRenderView world, BlockPos origin, T state, int x, int z, QuadView quad, QuadColorizer<T> resolver) {
        return resolver.getColor(state, world, this.mpos.set(x, origin.getY(), z), quad);
    }

    private <T> int getInterpolatedVertexColor(BlockRenderView world, BlockPos origin, T state,
                                               QuadView quad, int vertexIdx, QuadColorizer<T> resolver) {
        final float x = quad.x(vertexIdx);
        final float z = quad.z(vertexIdx);

        final int intX = (int) x;
        final int intZ = (int) z;

        // Integer component of position vector
        final int originX = origin.getX() + intX;
        final int originZ = origin.getZ() + intZ;

        // Retrieve the color values for each neighbor
        final int c1 = this.getBlockColor(world, origin, state, originX, originZ, quad, resolver);
        final int c2 = this.getBlockColor(world, origin, state, originX, originZ + 1, quad, resolver);
        final int c3 = this.getBlockColor(world, origin, state, originX + 1, originZ, quad, resolver);
        final int c4 = this.getBlockColor(world, origin, state, originX + 1, originZ + 1, quad, resolver);

        final int result;

        // All the colors are the same, so the results of interpolation will be useless.
        if (c1 == c2 && c2 == c3 && c3 == c4) {
            result = c1;
        } else {
            // Fraction component of position vector
            final float fracX = x - intX;
            final float fracZ = z - intZ;

            int z1 = ColorMixer.getStartRatio(fracZ);
            int z2 = ColorMixer.getEndRatio(fracZ);

            int r1 = ColorMixer.mixARGB(c1, c2, z1, z2);
            int r2 = ColorMixer.mixARGB(c3, c4, z1, z2);

            int x1 = ColorMixer.getStartRatio(fracX);
            int x2 = ColorMixer.getEndRatio(fracX);

            result = ColorMixer.mixARGB(r1, r2, x1, x2);
        }

        return result;
    }

}
