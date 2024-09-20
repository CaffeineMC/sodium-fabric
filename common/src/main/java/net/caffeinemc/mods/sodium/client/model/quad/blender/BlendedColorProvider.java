package net.caffeinemc.mods.sodium.client.model.quad.blender;

import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.util.ColorMixer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

public abstract class BlendedColorProvider<T> implements ColorProvider<T> {
    @Override
    public void getColors(LevelSlice slice, BlockPos pos, BlockPos.MutableBlockPos scratchPos, T state, ModelQuadView quad, int[] output) {
        for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
            output[vertexIndex] = this.getVertexColor(slice, pos, scratchPos, quad, state, vertexIndex);
        }
    }

    private int getVertexColor(LevelSlice slice, BlockPos pos, BlockPos.MutableBlockPos scratchPos, ModelQuadView quad, T state, int vertexIndex) {
        // Offset the position by -0.5f to align smooth blending with flat blending.
        final float posX = quad.getX(vertexIndex) - 0.5f;
        final float posY = quad.getY(vertexIndex) - 0.5f;
        final float posZ = quad.getZ(vertexIndex) - 0.5f;

        // Floor the positions here to always get the largest integer below the input
        // as negative values by default round toward zero when casting to an integer.
        // Which would cause negative ratios to be calculated in the interpolation later on.
        final int posIntX = Mth.floor(posX);
        final int posIntY = Mth.floor(posY);
        final int posIntZ = Mth.floor(posZ);

        // Integer component of position vector
        final int blockIntX = pos.getX() + posIntX;
        final int blockIntY = pos.getY() + posIntY;
        final int blockIntZ = pos.getZ() + posIntZ;

        // Retrieve the color values for each neighboring block
        final int c00 = this.getColor(slice, state, scratchPos.set(blockIntX + 0, blockIntY, blockIntZ + 0));
        final int c01 = this.getColor(slice, state, scratchPos.set(blockIntX + 0, blockIntY, blockIntZ + 1));
        final int c10 = this.getColor(slice, state, scratchPos.set(blockIntX + 1, blockIntY, blockIntZ + 0));
        final int c11 = this.getColor(slice, state, scratchPos.set(blockIntX + 1, blockIntY, blockIntZ + 1));

        // Linear interpolation across the Z-axis
        int z0;

        if (c00 != c01) {
            z0 = ColorMixer.mix(c00, c01, posZ - posIntZ);
        } else {
            z0 = c00;
        }

        int z1;

        if (c10 != c11) {
            z1 = ColorMixer.mix(c10, c11, posZ - posIntZ);
        } else {
            z1 = c10;
        }

        // Linear interpolation across the X-axis
        int x0;

        if (z0 != z1) {
            x0 = ColorMixer.mix(z0, z1, posX - posIntX);
        } else {
            x0 = z0;
        }

        return x0;
    }

    protected abstract int getColor(LevelSlice slice, T state, BlockPos pos);
}
