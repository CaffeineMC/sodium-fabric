package me.jellysquid.mods.sodium.client.render.model.quad.blender;

import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public interface VertexColorBlender {
    int getColor(BlockColorProvider provider, BlockState state, BlockRenderView world, int color, int colorIndex, float posX, float posZ, BlockPos origin, float brightness);
}
