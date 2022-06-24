package net.caffeinemc.sodium.interop.vanilla.block;

import net.minecraft.state.State;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public interface BlockColorSettings<T> {
    /**
     * Configures whether colors from a color provider will be interpolated for this block. You should only
     * enable this functionality if your color provider returns values based upon a pair of coordinates in the world,
     * and not if it needs access to the block state itself.
     *
     * @return True if interpolation should be used, otherwise false.
     */
    boolean useSmoothColorBlending(BlockRenderView view, T state, BlockPos pos);

    @SuppressWarnings("unchecked")
    static <T> boolean isSmoothBlendingEnabled(BlockRenderView world, State<T, ?> state, BlockPos pos) {
        if (state.owner instanceof BlockColorSettings settings) {
            return settings.useSmoothColorBlending(world, state, pos);
        }

        return false;
    }
}