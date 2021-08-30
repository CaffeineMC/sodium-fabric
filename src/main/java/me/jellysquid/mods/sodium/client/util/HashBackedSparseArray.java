package me.jellysquid.mods.sodium.client.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.Arrays;
import java.util.function.IntFunction;

public class HashBackedSparseArray<T> {
    private final T[][] buckets;
    private final Int2ObjectMap<T> hash = new Int2ObjectOpenHashMap<>();

    private final int indexFactor;
    private final int indexMask;

    private final int bucketSize;
    private final int capacity;

    @SuppressWarnings("unchecked")
    public HashBackedSparseArray(int capacity, int factor) {
        this.buckets = (T[][]) new Object[capacity >> factor][];
        this.capacity = capacity;
        this.indexFactor = factor;
        this.bucketSize = 1 << factor;
        this.indexMask = this.bucketSize - 1;
    }

    public void clear() {
        Arrays.fill(this.buckets, null);
    }

    public T get(int index) {
        if (index >= this.capacity) {
            return this.getHashed(index);
        }

        T[] bucket = this.buckets[index >> this.indexFactor];

        if (bucket == null) {
            return null;
        }

        return bucket[index & this.indexMask];
    }

    private T getHashed(int index) {
        return this.hash.get(index);
    }

    @SuppressWarnings("unchecked")
    public void put(int index, T value)  {
        if (index >= this.capacity) {
            this.putHashed(index, value);
            return;
        }

        int bucketIndex = index >> this.indexFactor;
        T[] bucket = this.buckets[bucketIndex];

        if (bucket == null) {
            this.buckets[bucketIndex] = (bucket = (T[]) new Object[this.bucketSize]);
        }

        bucket[index & this.indexMask] = value;
    }

    private void putHashed(int index, T value) {
        this.hash.put(index, value);
    }
}
