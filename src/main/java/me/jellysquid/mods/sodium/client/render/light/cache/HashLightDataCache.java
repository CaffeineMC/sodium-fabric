package me.jellysquid.mods.sodium.client.render.light.cache;

import it.unimi.dsi.fastutil.longs.Long2LongLinkedOpenHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

/**
 * A light data cache which uses a hash table to store previously accessed values.
 */
public class HashLightDataCache extends LightDataCache {
    private final Long2LongLinkedOpenHashMap map = new Long2LongLinkedOpenHashMap(1024, 0.50f);

    public void init(BlockRenderView world) {
        this.world = world;
    }

    @Override
    public long get(int x, int y, int z) {
        long key = BlockPos.asLong(x, y, z);
        long word = this.map.getAndMoveToFirst(key);

        if (word == 0) {
            if (this.map.size() > 1024) {
                this.map.removeLastLong();
            }

            this.map.put(key, word = this.compute(x, y, z));
        }

        return word;
    }

    public void clear() {
        this.map.clear();
    }
}