package me.jellysquid.mods.sodium.common.util;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;

import java.util.Arrays;

public class IdTable<T> {
    private final IntArrayFIFOQueue freeIds = new IntArrayFIFOQueue();

    private T[] elements;
    private int nextId;
    private int capacity;

    @SuppressWarnings("unchecked")
    public IdTable(int capacity) {
        this.elements = (T[]) new Object[capacity];
        this.capacity = capacity;
    }

    public int add(T element) {
        int id = this.allocateId();

        if (id >= this.capacity) {
            this.grow();
        }

        this.elements[id] = element;

        return id;
    }

    private void grow() {
        this.elements = Arrays.copyOf(this.elements, this.capacity *= 2);
    }

    public void remove(int id) {
        this.elements[id] = null;
        this.freeIds.enqueue(id);
    }

    private int allocateId() {
        if (!this.freeIds.isEmpty()) {
            return this.freeIds.dequeueInt();
        }

        return this.nextId++;
    }

    public T get(int id) {
        return this.elements[id];
    }

    public void set(int id, T value) {
        this.elements[id] = value;
    }
}
