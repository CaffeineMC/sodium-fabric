package me.jellysquid.mods.sodium.client.model.quad.blender;

import me.jellysquid.mods.sodium.client.model.quad.QuadColorizer;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

import java.util.Arrays;

/**
 * A simple colorizer which performs no blending between adjacent blocks.
 */
public class FlatBiomeColorBlender implements BiomeColorBlender {
    private final int[] cachedRet = new int[4];

    @Override
    public <T> int[] getColors(BlockRenderView world, BlockPos origin, QuadView quad, QuadColorizer<T> colorizer, T state) {
        Arrays.fill(this.cachedRet, colorizer.getColor(state, world, origin, quad.colorIndex()));

        return this.cachedRet;
    }
}
