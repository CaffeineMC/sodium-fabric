package me.jellysquid.mods.sodium.client.model.light.data;

import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;

/**
 * A light data cache which uses a hash table to store previously accessed values.
 */
public class HashLightDataCache extends LightDataAccess {
    private final Long2IntLinkedOpenHashMap map = new Long2IntLinkedOpenHashMap(1024, 0.50f);

    public HashLightDataCache(BlockAndTintGetter world) {
        this.world = world;
    }

    @Override
    public int get(int x, int y, int z) {
        long key = BlockPos.asLong(x, y, z);
        int word = this.map.getAndMoveToFirst(key);

        if (word == 0) {
            if (this.map.size() > 1024) {
                this.map.removeLastInt();
            }

            this.map.put(key, word = this.compute(x, y, z));
        }

        return word;
    }

    public void clearCache() {
        this.map.clear();
    }
}