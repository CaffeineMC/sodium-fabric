package net.caffeinemc.sodium.mixin.features.fast_biome_colors;

import net.caffeinemc.sodium.interop.vanilla.block.BlockColorSettings;
import net.caffeinemc.sodium.interop.vanilla.block.DefaultBlockColorSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Block.class)
public class MixinBlock implements BlockColorSettings<BlockState> {
    @Override
    public boolean useSmoothColorBlending(BlockRenderView view, BlockState state, BlockPos pos) {
        return DefaultBlockColorSettings.isSmoothBlendingAvailable(state.getBlock());
    }
}