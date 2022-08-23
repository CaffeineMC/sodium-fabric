package me.jellysquid.mods.sodium.client.world.biome;

import me.jellysquid.mods.sodium.client.model.quad.blender.ColorSampler;
import net.minecraft.block.BlockState;

public interface BlockColorsExtended {
    ColorSampler<BlockState> getColorProvider(BlockState state);
}
