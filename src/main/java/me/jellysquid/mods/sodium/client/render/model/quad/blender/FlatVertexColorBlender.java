package me.jellysquid.mods.sodium.client.render.model.quad.blender;

import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.util.ColorUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public class FlatVertexColorBlender implements VertexColorBlender {
    private final int[] cachedRet = new int[4];

    @Override
    public int[] getColors(BlockColorProvider provider, BlockState state, BlockRenderView world, ModelQuadView quad, BlockPos origin, int colorIndex, float[] brightness) {
        int biomeColor = provider.getColor(state, world, origin, colorIndex);

        float r = ColorUtil.normalize(ColorUtil.unpackColorR(biomeColor));
        float g = ColorUtil.normalize(ColorUtil.unpackColorG(biomeColor));
        float b = ColorUtil.normalize(ColorUtil.unpackColorB(biomeColor));

        int[] colors = this.cachedRet;

        for (int i = 0; i < 4; i++) {
            colors[i] = ColorUtil.mulPackedRGB(quad.getColor(i), r * brightness[i],  g * brightness[i], b * brightness[i]);
        }

        return colors;
    }
}
