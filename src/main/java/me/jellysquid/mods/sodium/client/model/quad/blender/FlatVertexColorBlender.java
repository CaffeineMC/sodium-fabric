package me.jellysquid.mods.sodium.client.model.quad.blender;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.util.ColorARGB;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

/**
 * A simple colorizer which performs no blending between adjacent blocks.
 */
public class FlatVertexColorBlender implements VertexColorBlender {
    private final int[] cachedRet = new int[4];

    @Override
    public int[] getColors(BlockColorProvider provider, BlockState state, BlockRenderView world, ModelQuadView quad, BlockPos origin, int colorIndex, float[] brightness) {
        int biomeColor = provider.getColor(state, world, origin, colorIndex);

        float r = ColorARGB.normalize(ColorARGB.unpackRed(biomeColor));
        float g = ColorARGB.normalize(ColorARGB.unpackGreen(biomeColor));
        float b = ColorARGB.normalize(ColorARGB.unpackBlue(biomeColor));

        int[] colors = this.cachedRet;

        for (int i = 0; i < 4; i++) {
            colors[i] = ColorARGB.mulPacked(quad.getColor(i), r * brightness[i],  g * brightness[i], b * brightness[i]);
        }

        return colors;
    }
}
