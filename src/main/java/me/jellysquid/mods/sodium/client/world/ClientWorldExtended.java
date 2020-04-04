package me.jellysquid.mods.sodium.client.world;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.level.ColorResolver;

public interface ClientWorldExtended {
    // [VanillaCopy] ClientWorld#getColor(BlockPos, ColorResolver)
    // Allows passing a custom BiomeAccess which will be used to retrieve biome data from
    int getColor(BlockPos pos, ColorResolver resolver, BiomeAccess biomeAccess);
}
