package me.jellysquid.mods.sodium.client.world.biome;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadColorProvider;
import net.minecraft.block.BlockState;

public interface BlockColorsExtended {
    ModelQuadColorProvider<BlockState> getColorProvider(BlockState state);
}
