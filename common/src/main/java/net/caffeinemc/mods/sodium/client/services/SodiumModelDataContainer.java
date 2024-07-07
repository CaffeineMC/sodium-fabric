package net.caffeinemc.mods.sodium.client.services;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.core.BlockPos;

import java.util.Map;

/**
 * A container that holds the platform's model data.
 */
public class SodiumModelDataContainer {
    private final Long2ObjectMap<SodiumModelData> modelDataMap;

    public SodiumModelDataContainer(Long2ObjectMap<SodiumModelData> modelDataMap) {
        this.modelDataMap = modelDataMap;
    }

    public SodiumModelData getModelData(BlockPos pos) {
        return modelDataMap.getOrDefault(pos.asLong(), SodiumModelData.EMPTY);
    }
}
