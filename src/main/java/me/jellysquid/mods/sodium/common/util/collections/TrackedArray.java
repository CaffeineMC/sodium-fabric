package me.jellysquid.mods.sodium.common.util.collections;

import java.lang.reflect.Array;
import java.util.Arrays;

public class TrackedArray<T extends TrackedArrayItem> {
    private T[] array;
    private int capacity;

    @SuppressWarnings("unchecked")
    public TrackedArray(Class<T> type, int size) {
        this.array = (T[]) Array.newInstance(type, size);
        this.capacity = size;
    }

    public void add(T item) {
        this.add(item.getId(), item);
    }

    private void add(int id, T item) {
        if (id >= this.capacity) {
            this.grow(id);
        }

        this.array[id] = item;
    }

    public void remove(T item) {
        this.remove(item.getId());
    }

    public T remove(int id) {
        T prev = this.array[id];
        this.array[id] = null;

        return prev;
    }

    private void grow(int min) {
        int size = Math.max(this.array.length * 2, min);

        this.array = Arrays.copyOf(this.array, size);
        this.capacity = size;
    }

    public T get(int id) {
        return this.array[id];
    }
}
