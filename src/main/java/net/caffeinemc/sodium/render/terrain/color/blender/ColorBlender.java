package net.caffeinemc.sodium.render.terrain.color.blender;

import net.caffeinemc.sodium.render.terrain.color.ColorSampler;
import net.caffeinemc.sodium.render.terrain.quad.ModelQuadView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.state.State;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public interface ColorBlender {
    /**
     * Computes the per-vertex colors of a model quad.
     *
     * The array returned by this method may be re-used in subsequent calls in order to reduce memory allocations, and
     * as such, the contents of an array returned by this method are undefined after a subsequent call.
     *
     * @param world The world to sample biomes (and as a result, colors) from
     * @param origin The position of the block being rendered
     * @param quad The quad which will be colorized
     * @param sampler The source from which color will be sampled
     * @param state The block state being rendered
     * @return An array of ABGR colors
     */
    <T extends State<O, ?>, O> int[] getColors(BlockRenderView world, BlockPos origin, ModelQuadView quad, ColorSampler<T> sampler, T state);

    static ColorBlender create(MinecraftClient client) {
        return new ConfigurableColorBlender(client);
    }
}
