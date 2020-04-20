package me.jellysquid.mods.sodium.client.render.model.quad.blender;

import me.jellysquid.mods.sodium.client.util.ColorUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public class FlatVertexColorBlender implements VertexColorBlender {
    @Override
    public int getColor(BlockColorProvider provider, BlockState state, BlockRenderView world, int color, int colorIndex, float posX, float posZ, BlockPos origin, float brightness) {
        int biomeColor = provider.getColor(state, world, origin, colorIndex);

        float r = ColorUtil.normalize(ColorUtil.unpackColorR(biomeColor));
        float g = ColorUtil.normalize(ColorUtil.unpackColorG(biomeColor));
        float b = ColorUtil.normalize(ColorUtil.unpackColorB(biomeColor));

        return ColorUtil.mulPackedRGB(color, r * brightness,  g * brightness, b * brightness);
    }
}
