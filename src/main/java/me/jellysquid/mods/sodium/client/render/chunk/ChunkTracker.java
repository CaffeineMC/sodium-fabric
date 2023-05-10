package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.util.math.ChunkPos;

import java.util.stream.LongStream;

public class ChunkTracker {
    private final Long2BooleanOpenHashMap single = new Long2BooleanOpenHashMap();
    private final Long2BooleanOpenHashMap merged = new Long2BooleanOpenHashMap();

    private final LongLinkedOpenHashSet dirty = new LongLinkedOpenHashSet();

    public ChunkTracker() {
        this.single.defaultReturnValue(false);
        this.merged.defaultReturnValue(false);
    }

    public void update() {
        if (this.dirty.isEmpty()) {
            return;
        }

        var dirty = this.markDirtyChunks();
        this.recalculateChunks(dirty);

        this.dirty.clear();
    }

    private void recalculateChunks(LongSet set) {
        LongIterator it = set.iterator();

        while (it.hasNext()) {
            long key = it.nextLong();

            var x = ChunkPos.getPackedX(key);
            var z = ChunkPos.getPackedZ(key);

            boolean flags = this.single.get(key);

            for (int ox = -1; ox <= 1; ox++) {
                for (int oz = -1; oz <= 1; oz++) {
                    flags &= this.single.get(ChunkPos.toLong(ox + x, oz + z));
                }
            }

            if (flags) {
                this.merged.put(key, flags);
            } else {
                this.merged.remove(key);
            }
        }
    }

    private LongSet markDirtyChunks() {
        var dirty = new LongOpenHashSet(this.dirty);
        var it = this.dirty.iterator();

        while (it.hasNext()) {
            var key = it.nextLong();
            var x = ChunkPos.getPackedX(key);
            var z = ChunkPos.getPackedZ(key);

            for (int ox = -1; ox <= 1; ox++) {
                for (int oz = -1; oz <= 1; oz++) {
                    dirty.add(ChunkPos.toLong(ox + x, oz + z));
                }
            }
        }

        return dirty;
    }

    public boolean loadChunk(int x, int z) {
        var key = ChunkPos.toLong(x, z);

        if (this.single.put(key, true)) {
            return false;
        }

        this.dirty.add(key);

        return true;
    }

    public boolean unloadChunk(int x, int z) {
        long key = ChunkPos.toLong(x, z);

        if (!this.single.remove(key)) {
            return false;
        }

        this.dirty.add(key);

        return true;
    }

    public boolean hasData(int x, int z) {
        return this.merged.get(ChunkPos.toLong(x, z));
    }

    public LongStream getChunks() {
        return this.single
                .long2BooleanEntrySet()
                .stream()
                .filter(entry -> (entry.getBooleanValue()))
                .mapToLong(Long2BooleanMap.Entry::getLongKey);
    }
}
