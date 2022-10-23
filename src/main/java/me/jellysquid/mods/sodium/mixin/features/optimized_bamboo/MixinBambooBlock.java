package me.jellysquid.mods.sodium.mixin.features.optimized_bamboo;

import net.minecraft.block.BambooBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BambooBlock.class)
public class MixinBambooBlock extends Block {
    public MixinBambooBlock(Settings settings) {
        super(settings);
    }

    // This is a fix for an oversight on Mojang's side, where this block always returns 1.0 regardless of the state. It returns the same result,
    // but improves performance significantly. This was originally found by darkevilmac in https://github.com/TridentMC/FastBamboo.
    @SuppressWarnings("deprecation")
    @Override
    public float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
        return 1.0f;
    }
}
