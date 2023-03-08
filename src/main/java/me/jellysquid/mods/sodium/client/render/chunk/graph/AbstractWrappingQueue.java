package me.jellysquid.mods.sodium.client.render.chunk.graph;

import net.minecraft.util.math.MathHelper;

import java.util.NoSuchElementException;

public abstract class AbstractWrappingQueue {
    private int head, tail;
    private int capacity;

    private int mask;

    public AbstractWrappingQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("The initial capacity must be non-zero");
        }

        capacity = MathHelper.smallestEncompassingPowerOfTwo(capacity);

        this.capacity = capacity;
        this.mask = capacity - 1;
    }

    /**
     * Reserves space for an entry that will be added to the queue later. This should only be called if an entry will
     * be immediately stored at the reserved index, otherwise the queue could read out uninitialized entries!
     *
     * @return The reserved index at which elements which are about to be added should be stored at
     */
    protected final int reserveNext() {
        final int cur = this.tail;

        var next = inc(this.tail, this.mask);

        if (next == this.head) {
            throw new ArrayIndexOutOfBoundsException("Too many entries are in queue");
        }

        this.tail = next;

        return cur;
    }

    /**
     * @return True if the queue has no more entries, otherwise false
     */
    public final boolean isEmpty() {
        return this.head == this.tail;
    }

    /**
     * Clears the queue of entries and ensures all referenced resources are released.
     */
    public final void clear() {
        for (int index = this.head; index != this.tail; index = inc(index, this.mask)) {
            this.erase(index);
        }

        this.head = 0;
        this.tail = 0;
    }

    /**
     * Erases the entry at {@param index}. Strictly speaking, this is not necessary, but if objects are stored
     * in the queue, this prevents their references from being kept around.
     */
    protected abstract void erase(int index);

    /**
     * Advances the queue forward one entry, and erases the previous entry.
     */
    public final void next() {
        if (!this.isEmpty()) {
            this.head = inc(this.head, this.mask);
        }
    }

    public int advanceIndex() {
        var cur = this.head;

        if (cur == this.tail) {
            throw new NoSuchElementException();
        }

        this.head = inc(cur, this.mask);

        return cur;
    }

    /**
     * @return The current size of the element arrays.
     */
    protected final int capacity() {
        return this.capacity;
    }

    /**
     * Increments {@param index} by one, wrapping it around according to the {@param mask}. This is the same
     * as {@code (index + 1) % capacity} but only works when the capacity is a power of two.
     */
    private static int inc(int index, int mask) {
        return (index + 1) & mask;
    }
}
