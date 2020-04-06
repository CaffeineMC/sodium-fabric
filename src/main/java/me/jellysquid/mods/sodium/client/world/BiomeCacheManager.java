package me.jellysquid.mods.sodium.client.world;

import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkSlice;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.biome.source.BiomeAccessType;

import java.util.concurrent.locks.StampedLock;

public class BiomeCacheManager {
    private final Long2ReferenceLinkedOpenHashMap<BiomeCache> caches = new Long2ReferenceLinkedOpenHashMap<>();
    private final StampedLock lock = new StampedLock();

    private final BiomeAccessType type;
    private final long seed;

    public BiomeCacheManager(BiomeAccessType type, long seed) {
        this.type = type;
        this.seed = seed;
    }

    public BiomeCache[] getCacheArray(ChunkSectionPos chunkPos) {
        return this.getCacheArray(chunkPos.getX(), chunkPos.getY(), chunkPos.getZ());
    }

    public BiomeCache[] getCacheArray(int centerX, int centerY, int centerZ) {
        BiomeCache[] cachesArray = new BiomeCache[3 * 3 * 3];

        int minX = centerX - 1;
        int minY = centerY - 1;
        int minZ = centerZ - 1;

        int maxX = centerX + 1;
        int maxY = centerY + 1;
        int maxZ = centerZ + 1;

        long stamp = this.lock.writeLock();

        try {
            final Long2ReferenceLinkedOpenHashMap<BiomeCache> caches = this.caches;

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        long key = ChunkSectionPos.asLong(x, y, z);

                        BiomeCache cache = caches.getAndMoveToFirst(key);

                        if (cache == null) {
                            caches.put(key, cache = new BiomeCache(this.type, this.seed));

                            if (caches.size() > 256) {
                                caches.removeLast();
                            }
                        }

                        cachesArray[ChunkSlice.getSectionIndex(x - minX, y - minY, z - minZ)] = cache;
                    }
                }
            }
        } finally {
            this.lock.unlockWrite(stamp);
        }

        return cachesArray;
    }

    public void clearCacheFor(int x, int z) {
        long stamp = this.lock.writeLock();

        try {
            for (int y = 0; y < 16; y++) {
                this.caches.remove(ChunkSectionPos.asLong(x, y, z));
            }
        } finally {
            this.lock.unlockWrite(stamp);
        }
    }
}