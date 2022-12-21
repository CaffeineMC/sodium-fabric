package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.util.math.ChunkPos;

import java.util.stream.LongStream;

public class ChunkTracker {
    private final Long2IntOpenHashMap single = new Long2IntOpenHashMap();
    private final Long2IntOpenHashMap merged = new Long2IntOpenHashMap();

    private final Callback callback;


    public ChunkTracker(Callback callback) {
        this.single.defaultReturnValue(0);
        this.merged.defaultReturnValue(0);

        this.callback = callback;
    }

    private void updateMerged(int x, int z) {
        long key = ChunkPos.toLong(x, z);

        int flags = this.single.get(key);

        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                flags &= this.single.get(ChunkPos.toLong(ox + x, oz + z));
            }
        }

        int prev;

        if (flags != 0) {
            prev = this.merged.put(key, flags);
        } else {
            prev = this.merged.remove(key);
        }

        if (prev != flags) {
            if (prev == ChunkStatus.FLAG_ALL) {
                this.callback.unloadChunk(x, z);
            } else if (flags == ChunkStatus.FLAG_ALL) {
                this.callback.loadChunk(x, z);
            }
        }
    }

    public void mark(int x, int z, int bits) {
        var key = ChunkPos.toLong(x, z);
        var prev = this.single.get(key);

        if ((prev & bits) == bits) {
            return;
        }

        this.single.put(key, prev | bits);

        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                this.updateMerged(ox + x, oz + z);
            }
        }
    }

    public void remove(int x, int z) {
        var key = ChunkPos.toLong(x, z);
        var prev = this.single.get(key);

        if (prev == 0) {
            return;
        }

        this.single.remove(key);

        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                this.updateMerged(ox + x, oz + z);
            }
        }
    }

    public boolean hasMergedFlags(int x, int z, int flags) {
        return (this.merged.get(ChunkPos.toLong(x, z)) & flags) == flags;
    }

    public LongStream getLoadedChunks() {
        return this.merged
                .long2IntEntrySet()
                .stream()
                .filter(entry -> entry.getIntValue() == ChunkStatus.FLAG_ALL)
                .mapToLong(Long2IntMap.Entry::getLongKey);
    }

    public interface Callback {
        void loadChunk(int x, int z);
        void unloadChunk(int x, int z);
    }
}
