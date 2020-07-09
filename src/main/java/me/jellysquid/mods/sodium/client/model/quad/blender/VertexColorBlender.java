package me.jellysquid.mods.sodium.client.model.quad.blender;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

/**
 *
 */
public interface VertexColorBlender {
    /**
     * Computes the blended biome colors and returns an an array containing the integer-encoded colors for each vertex.
     * The array returned by this method may be re-used in subsequent calls in order to reduce memory allocations, and
     * as such, the contents of an array returned by this method is undefined after a subsequent call.
     *
     * @param provider The color provider which will retrieve the colorized
     * @param state The block state of the block being rendered in the world
     * @param world The world context of this rendered block
     * @param quad The quad which will be colorized
     * @param origin The position of the block being rendered
     * @param colorIndex The color index for this vertex which will be passed to the color provider
     * @param brightness The array of brightness
     *
     * @return An array of integer colors in ABGR format
     */
    int[] getColors(BlockColorProvider provider, BlockState state, BlockRenderView world, ModelQuadView quad,
                    BlockPos origin, int colorIndex, float[] brightness);
}
