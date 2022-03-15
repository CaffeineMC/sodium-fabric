package net.caffeinemc.sodium.interop.vanilla.mixin;

import net.caffeinemc.sodium.render.terrain.color.ColorSampler;
import net.minecraft.block.BlockState;

public interface BlockColorProviderRegistry {
    ColorSampler<BlockState> getColorProvider(BlockState state);
}
