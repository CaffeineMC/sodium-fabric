package me.jellysquid.mods.sodium.common.util.collections;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;

public class IntPool {
    private final IntArrayFIFOQueue freeIds;
    private int nextId;

    public IntPool() {
        this.freeIds = new IntArrayFIFOQueue(32);
        this.nextId = 0;
    }

    public int allocateId() {
        if (!this.freeIds.isEmpty()) {
            return this.freeIds.dequeueInt();
        }

        return this.nextId++;
    }

    public void deallocateId(int id) {
        this.freeIds.enqueue(id);
    }
}
