package me.jellysquid.mods.sodium.client.model.quad.blender;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

import java.util.Arrays;

/**
 * A simple color blender which uses the same color for all corners.
 */
public class FlatColorBlender implements ColorBlender {
    private final int[] cachedRet = new int[4];

    @Override
    public <T> int[] getColors(BlockRenderView world, BlockPos origin, ModelQuadView quad, ColorSampler<T> sampler, T state) {
        Arrays.fill(this.cachedRet, ColorARGB.toABGR(sampler.getColor(state, world, origin, quad.getColorIndex())));

        return this.cachedRet;
    }
}
