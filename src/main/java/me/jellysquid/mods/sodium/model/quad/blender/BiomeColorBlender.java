package me.jellysquid.mods.sodium.model.quad.blender;

import me.jellysquid.mods.sodium.model.quad.QuadColorizer;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public interface BiomeColorBlender {
    /**
     * Computes the blended biome colors and returns an array containing the integer-encoded colors for each vertex.
     * The array returned by this method may be re-used in subsequent calls in order to reduce memory allocations, and
     * as such, the contents of an array returned by this method is undefined after a subsequent call.
     *
     * @param world The world to sample biomes (and as a result, colors) from
     * @param origin The position of the block being rendered
     * @param quad The quad which will be colorized
     * @param colorizer The color sampling source
     * @param state The block state being rendered
     * @return An array of integer colors in ARGB format
     */
    <T> int[] getColors(BlockRenderView world, BlockPos origin, QuadView quad, QuadColorizer<T> colorizer, T state);

    static BiomeColorBlender create(MinecraftClient client) {
        return client.options.biomeBlendRadius > 0 ? new SmoothBiomeColorBlender() : new FlatBiomeColorBlender();
    }
}
