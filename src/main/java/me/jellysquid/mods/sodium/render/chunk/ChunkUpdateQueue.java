package me.jellysquid.mods.sodium.render.chunk;

import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.util.math.ChunkSectionPos;

public class ChunkUpdateQueue {
    private static final int MAX_BUCKETS_XZ = 32;
    private static final int MAX_BUCKETS_Y = 16;

    private static final int MAX_BUCKETS = (MAX_BUCKETS_XZ * 2) + MAX_BUCKETS_Y;

    private final Long2ReferenceMap<ChunkUpdateType> entries = new Long2ReferenceOpenHashMap<>();
    private final LongPriorityQueue[][] sortedQueues = new LongPriorityQueue[ChunkUpdateType.COUNT][MAX_BUCKETS + 1];
    private final LongSet[] tables = new LongSet[ChunkUpdateType.COUNT];

    public ChunkUpdateQueue() {
        for (LongPriorityQueue[] queues : this.sortedQueues) {
            for (int i = 0; i < queues.length; i++) {
                queues[i] = new OrderedQueue();
            }
        }

        for (int i = 0; i < this.tables.length; i++) {
            this.tables[i] = new LongOpenHashSet();
        }
    }

    public LongPriorityQueue[][] sort(int centerX, int centerY, int centerZ) {
        clearQueues(this.sortedQueues);

        for (int tableIndex = 0; tableIndex < this.tables.length; tableIndex++) {
            LongSet table = this.tables[tableIndex];
            LongIterator it = table.iterator();

            var queues = this.sortedQueues[tableIndex];

            while (it.hasNext()) {
                var key = it.nextLong();

                int distance = 0;
                distance += Math.abs(ChunkSectionPos.unpackX(key) - centerX);
                distance += Math.abs(ChunkSectionPos.unpackY(key) - centerY);
                distance += Math.abs(ChunkSectionPos.unpackZ(key) - centerZ);

                if (distance > MAX_BUCKETS) {
                    distance = MAX_BUCKETS;
                }

                var list = queues[distance];
                list.enqueue(key);
            }
        }

        return this.sortedQueues;
    }

    private static void clearQueues(LongPriorityQueue[][] queueBiArray) {
        for (LongPriorityQueue[] queueArray : queueBiArray) {
            for (LongPriorityQueue queue : queueArray) {
                queue.clear();
            }
        }
    }

    public void add(int x, int y, int z, ChunkUpdateType type) {
        var key = ChunkSectionPos.asLong(x, y, z);
        var prev = this.entries.get(key);

        if (prev == null || prev.ordinal() < type.ordinal()) {
            if (prev != null) {
                this.tables[prev.ordinal()]
                        .remove(key);
            }

            var table = this.tables[type.ordinal()];
            table.add(key);

            this.entries.put(key, type);
        }
    }

    public void remove(long pos) {
        var type = this.entries.remove(pos);

        if (type != null) {
            this.tables[type.ordinal()]
                    .remove(pos);
        }
    }

    public void clear() {
        this.entries.clear();

        clearQueues(this.sortedQueues);
    }

    public void remove(int x, int y, int z) {
        this.remove(ChunkSectionPos.asLong(x, y, z));
    }

    public boolean isQueued(int x, int y, int z) {
        return this.entries.containsKey(ChunkSectionPos.asLong(x, y, z));
    }

    private class OrderedQueue extends LongArrayFIFOQueue {
        @Override
        public long dequeueLong() {
            var value = super.dequeueLong();

            ChunkUpdateQueue.this.remove(value);

            return value;
        }
    }
}
