package me.jellysquid.mods.sodium.client.world.biome;

import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColorProvider;

public interface BlockColorsExtended {
    BlockColorProvider getColorProvider(BlockState state);
}
