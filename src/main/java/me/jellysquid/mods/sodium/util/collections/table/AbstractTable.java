package me.jellysquid.mods.sodium.util.collections.table;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;

public class AbstractTable {
    private final IntArrayFIFOQueue freeIds = new IntArrayFIFOQueue();
    private int nextId = 1;

    protected int getNextId() {
        if (!this.freeIds.isEmpty()) {
            return this.freeIds.dequeueInt();
        } else {
            return this.nextId++;
        }
    }

    protected void freeId(int index) {
        this.freeIds.enqueue(index);
    }
}
