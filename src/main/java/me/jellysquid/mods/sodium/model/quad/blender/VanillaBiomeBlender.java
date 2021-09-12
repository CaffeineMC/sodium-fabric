package me.jellysquid.mods.sodium.model.quad.blender;

import me.jellysquid.mods.sodium.model.quad.QuadColorizer;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.state.State;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

import java.util.Arrays;

/**
 * A simple colorizer which performs no blending between adjacent blocks.
 */
class VanillaBiomeBlender implements BiomeBlender {
    @Override
    public <T extends State<O, ?>, O> void getColors(BlockRenderView world, T state, BlockPos origin, QuadView quad, QuadColorizer<T> resolver, int[] colors) {
        Arrays.fill(colors, resolver.getColor(state, world, origin, quad));
    }
}
