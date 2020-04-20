package me.jellysquid.mods.sodium.client.render.model.quad.blender;

import me.jellysquid.mods.sodium.client.util.ColorUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public class FlatVertexColorBlender implements VertexColorBlender {
    @Override
    public int getColor(BlockColorProvider provider, BlockState state, BlockRenderView world, int color, int colorIndex, float posX, float posZ, BlockPos origin, float brightness) {
        return ColorUtil.mulPackedRGB(provider.getColor(state, world, origin, colorIndex), brightness, brightness, brightness);
    }
}
