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
        final int next = this.tail;
        this.tail = inc(this.tail, this.mask);

        if (this.head == this.tail) {
            this.grow();
        }

        return next;
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
        this.erase(this.head);

        if (!this.isEmpty()) {
            this.head = inc(this.head, this.mask);
        }
    }

    /**
     * Increases the size of the queue's storage so that it can store (at least) one more entry. More space than
     * necessary may be allocated to avoid excessive resizing of the queue.
     */
    protected final void grow() {
        this.capacity <<= 1;
        this.mask = this.capacity - 1;

        this.resize(this.capacity);
    }

    /**
     * Resizes the arrays used for storing elements in the queue. The implementation should ensure that the arrays are
     * sized exactly to {@param capacity} and that elements in the old array are copied over.
     */
    protected abstract void resize(int capacity);

    /**
     * @return The current size of the element arrays.
     */
    protected final int capacity() {
        return this.capacity;
    }

    /**
     * @return The index of the element at the head of the queue
     * @throws NoSuchElementException If there are no elements remaining in the queue
     */
    protected final int currentElementIndex() {
        if (this.isEmpty()) {
            throw new NoSuchElementException();
        }

        return this.head;
    }

    /**
     * Increments {@param index} by one, wrapping it around according to the {@param mask}. This is the same
     * as {@code (index + 1) % capacity} but only works when the capacity is a power of two.
     */
    private static int inc(int index, int mask) {
        return (index + 1) & mask;
    }
}
