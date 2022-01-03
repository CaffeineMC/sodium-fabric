package me.jellysquid.mods.sodium.interop.vanilla.mixin;

import me.jellysquid.mods.sodium.render.terrain.color.ColorSampler;
import net.minecraft.block.BlockState;

public interface BlockColorProviderRegistry {
    ColorSampler<BlockState> getColorProvider(BlockState state);
}
