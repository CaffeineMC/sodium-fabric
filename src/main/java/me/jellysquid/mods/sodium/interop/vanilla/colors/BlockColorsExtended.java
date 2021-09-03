package me.jellysquid.mods.sodium.interop.vanilla.colors;

import me.jellysquid.mods.sodium.model.quad.QuadColorizer;
import net.minecraft.block.BlockState;

public interface BlockColorsExtended {
    QuadColorizer<BlockState> getColorProvider(BlockState state);
}
