package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;

public class IntPool {
    private final IntArrayFIFOQueue freeIds = new IntArrayFIFOQueue();
    private int size;

    public int create() {
        if (!this.freeIds.isEmpty()) {
            return this.freeIds.dequeueInt();
        }

        return this.size++;
    }

    public void free(int id) {
        this.freeIds.enqueue(id);
    }

    public void reset() {
        this.freeIds.clear();
        this.size = 0;
    }

    public int size() {
        return this.size;
    }
}