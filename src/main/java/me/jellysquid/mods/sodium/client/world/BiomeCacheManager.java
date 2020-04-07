package me.jellysquid.mods.sodium.client.world;

import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkSlice;
import me.jellysquid.mods.sodium.common.util.arena.Arena;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.source.BiomeAccessType;

public class BiomeCacheManager {
    private final Arena<BiomeCache> arena;
    private final Long2ReferenceLinkedOpenHashMap<BiomeCache[]> caches = new Long2ReferenceLinkedOpenHashMap<>(32, 0.5f);

    public BiomeCacheManager(BiomeAccessType type, long seed) {
        this.arena = new Arena<>(64, () -> new BiomeCache(type, seed));
    }

    public void populateArrays(int centerX, int centerY, int centerZ, BiomeCache[] array) {
        int minX = centerX - 1;
        int minZ = centerZ - 1;

        int maxX = centerX + 1;
        int maxZ = centerZ + 1;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                long key = ChunkPos.toLong(x, z);

                BiomeCache[] column = this.caches.getAndMoveToFirst(key);

                if (column == null) {
                    if (this.caches.size() >= 256) {
                        this.dropCacheColumn(this.caches.removeLast());
                    }

                    this.caches.put(key, column = new BiomeCache[16]);
                }

                BiomeCache cache = column[centerY];

                if (cache == null) {
                    column[centerY] = (cache = this.arena.allocate());
                }

                array[ChunkSlice.getChunkIndex(x - minX, z - minZ)] = cache;
            }
        }

        for (BiomeCache cache : array) {
            if (cache == null) {
                throw new IllegalStateException();
            }
        }
    }

    public void dropCachesForChunk(int centerX, int centerZ) {
        for (int x = centerX - 1; x <= centerX; x++) {
            for (int z = centerZ - 1; z <= centerZ; z++) {
                BiomeCache[] column = this.caches.remove(ChunkPos.toLong(x, z));

                if (column != null) {
                    this.dropCacheColumn(column);
                }
            }
        }
    }

    private void dropCacheColumn(BiomeCache[] column) {
        for (BiomeCache cache : column) {
            if (cache != null) {
                this.tryReclaimCache(cache);
            }
        }
    }

    private void tryReclaimCache(BiomeCache cache) {
        if (!cache.hasReferences()) {
            this.arena.reclaim(cache);
        }
    }

    public void release(BiomeCache cache) {
        cache.releaseReference();

        this.tryReclaimCache(cache);
    }
}