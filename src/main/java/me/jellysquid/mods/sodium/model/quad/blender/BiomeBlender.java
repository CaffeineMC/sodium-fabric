package me.jellysquid.mods.sodium.model.quad.blender;

import me.jellysquid.mods.sodium.model.quad.QuadColorizer;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.state.State;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public interface BiomeBlender {
    /**
     * Computes the blended biome colors and fill the provided array with the integer-encoded colors for each vertex.
     * The array returned by this method may be re-used in subsequent calls in order to reduce memory allocations, and
     * as such, the contents of an array returned by this method is undefined after a subsequent call.
     * @param world The world to sample biomes (and as a result, colors) from
     * @param origin The position of the block being rendered
     * @param quad The quad which will be colorized
     * @param resolver The color sampling source
     * @param colors The array in which output colors will be stored in
     */
    <T extends State<O, ?>, O> void getColors(BlockRenderView world, T state, BlockPos origin, QuadView quad, QuadColorizer<T> resolver, int[] colors);

    static BiomeBlender create(MinecraftClient client) {
        return new ConfigurableBiomeBlender(client);
    }
}
