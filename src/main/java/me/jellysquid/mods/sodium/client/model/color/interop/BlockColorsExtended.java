package me.jellysquid.mods.sodium.client.model.color.interop;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import net.minecraft.block.Block;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.client.color.block.BlockColors;

public interface BlockColorsExtended {
    static Iterable<Reference2ReferenceMap.Entry<Block, BlockColorProvider>> getProviders(BlockColors blockColors) {
        return ((BlockColorsExtended) blockColors).getProviders();
    }

    Iterable<Reference2ReferenceMap.Entry<Block, BlockColorProvider>> getProviders();
}
