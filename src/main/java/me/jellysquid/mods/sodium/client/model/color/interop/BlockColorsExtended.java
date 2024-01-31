package me.jellysquid.mods.sodium.client.model.color.interop;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import net.minecraft.block.Block;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.client.color.block.BlockColors;

public interface BlockColorsExtended {
    static Reference2ReferenceMap<Block, BlockColorProvider> getProviders(BlockColors blockColors) {
        return ((BlockColorsExtended) blockColors).sodium$getProviders();
    }

    static ReferenceSet<Block> getOverridenVanillaBlocks(BlockColors blockColors) {
        return ((BlockColorsExtended) blockColors).sodium$getOverridenVanillaBlocks();
    }

    Reference2ReferenceMap<Block, BlockColorProvider> sodium$getProviders();

    ReferenceSet<Block> sodium$getOverridenVanillaBlocks();
}
