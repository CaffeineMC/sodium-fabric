package me.jellysquid.mods.sodium.client.interop.vanilla.colors;

import me.jellysquid.mods.sodium.client.model.quad.QuadColorizer;
import net.minecraft.block.BlockState;

public interface BlockColorsExtended {
    QuadColorizer<BlockState> getColorProvider(BlockState state);
}
