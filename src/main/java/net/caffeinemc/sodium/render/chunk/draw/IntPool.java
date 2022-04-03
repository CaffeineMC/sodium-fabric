package net.caffeinemc.sodium.render.chunk.draw;

import it.unimi.dsi.fastutil.ints.*;

import java.util.NoSuchElementException;

public class IntPool {
    private final IntSet usedIds = new IntOpenHashSet();
    private final IntPriorityQueue freeIds = new IntArrayFIFOQueue();

    private int nextId = 1;

    public int create() {
        int id;

        if (this.freeIds.isEmpty()) {
            id = this.nextId++;
        } else {
            id = this.freeIds.dequeueInt();
        }

        this.usedIds.add(id);

        return id;
    }

    public void free(int id) {
        if (!this.usedIds.remove(id)) {
            throw new NoSuchElementException();
        }

        this.freeIds.enqueue(id);

        if (this.usedIds.isEmpty()) {
            this.nextId = 0;
        }
    }

    public int capacity() {
        return this.nextId;
    }
}
