package net.caffeinemc.sodium.render.terrain.color.blender;

import net.caffeinemc.sodium.render.terrain.color.ColorSampler;
import net.caffeinemc.sodium.render.terrain.quad.ModelQuadView;
import net.caffeinemc.sodium.util.packed.ColorARGB;
import net.minecraft.state.State;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

import java.util.Arrays;

/**
 * A simple color blender which uses the same color for all corners.
 */
public class FlatColorBlender implements ColorBlender {
    private final int[] cachedRet = new int[4];

    @Override
    public <T extends State<O, ?>, O> int[] getColors(BlockRenderView world, BlockPos origin, ModelQuadView quad, ColorSampler<T> sampler, T state) {
        Arrays.fill(this.cachedRet, ColorARGB.toABGR(sampler.getColor(state, world, origin, quad.getColorIndex())));

        return this.cachedRet;
    }
}
