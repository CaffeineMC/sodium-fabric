package net.caffeinemc.mods.sodium.client.model.color;

import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.core.BlockPos;

public interface ColorProvider<T> {
    /**
     * Computes the per-vertex colors of a model quad and stores the results in {@param output}. The order of
     * the output color array is the same as the order of the quad's vertices.
     *
     * @param slice  The level slice which contains the object being colorized
     * @param pos    The position of the object being colorized
     * @param scratchPos A mutable block position for temporary calculations
     * @param state  The state of the object being colorized
     * @param quad   The quad geometry which should be colorized
     * @param output The output array of vertex colors (in ABGR format)
     */
    void getColors(LevelSlice slice, BlockPos pos, BlockPos.MutableBlockPos scratchPos, T state, ModelQuadView quad, int[] output);
}
