package me.jellysquid.mods.sodium.client.world.biome;

import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.common.util.pool.ObjectPool;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.biome.source.BiomeAccessType;

public class BiomeCacheManager {
    private static final int CACHE_SIZE = 256;
    private static final int ARENA_SIZE = 64;

    private final ObjectPool<BiomeCache> pool;
    private final Long2ReferenceLinkedOpenHashMap<BiomeCache> caches = new Long2ReferenceLinkedOpenHashMap<>(CACHE_SIZE, 0.5f);

    public BiomeCacheManager(BiomeAccessType type, long seed) {
        this.pool = new ObjectPool<>(ARENA_SIZE, () -> new BiomeCache(type, seed));
    }

    public void populateArrays(int centerX, int centerY, int centerZ, BiomeCache[] array) {
        int minX = centerX - 1;
        int minZ = centerZ - 1;

        int maxX = centerX + 1;
        int maxZ = centerZ + 1;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                long key = ChunkSectionPos.asLong(x, centerY, z);

                BiomeCache cache = this.caches.getAndMoveToFirst(key);

                if (cache == null) {
                    if (this.caches.size() >= CACHE_SIZE) {
                        this.release(this.caches.removeLast());
                    }

                    this.caches.put(key, cache = this.pool.allocate());
                }

                this.pool.acquireReference(cache);

                array[WorldSlice.getLocalChunkIndex(x - minX, z - minZ)] = cache;
            }
        }
    }

    public void dropCachesForChunk(int centerX, int centerZ) {
        for (int x = centerX - 1; x <= centerX; x++) {
            for (int z = centerZ - 1; z <= centerZ; z++) {
                for (int y = 0; y <= 16; y++) {
                    BiomeCache column = this.caches.remove(ChunkSectionPos.asLong(x, y, z));

                    if (column != null) {
                        this.release(column);
                    }
                }
            }
        }
    }

    public void release(BiomeCache cache) {
        this.pool.release(cache);
    }
}