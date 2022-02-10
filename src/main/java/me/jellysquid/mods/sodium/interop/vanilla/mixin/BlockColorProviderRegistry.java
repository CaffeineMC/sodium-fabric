package me.jellysquid.mods.sodium.interop.vanilla.mixin;

import me.jellysquid.mods.sodium.render.terrain.color.ColorSampler;
import net.minecraft.world.level.block.state.BlockState;

public interface BlockColorProviderRegistry {
    ColorSampler<BlockState> getColorProvider(BlockState state);
}
