package me.jellysquid.mods.sodium.client.render.model.quad.blender;

import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadView;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public interface VertexColorBlender {
    int[] getColors(BlockColorProvider provider, BlockState state, BlockRenderView world, ModelQuadView quad, BlockPos origin, int colorIndex, float[] brightness);
}
