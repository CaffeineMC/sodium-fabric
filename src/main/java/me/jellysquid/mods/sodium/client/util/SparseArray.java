package me.jellysquid.mods.sodium.client.util;

import java.util.Arrays;
import java.util.function.IntFunction;

public class SparseArray<T> {
    private final T[][] buckets;

    private final int indexFactor;
    private final int indexMask;

    private final int bucketSize;

    @SuppressWarnings("unchecked")
    public SparseArray(int capacity, int factor) {
        this.buckets = (T[][]) new Object[capacity >> factor][];
        this.indexFactor = factor;
        this.bucketSize = 1 << factor;
        this.indexMask = this.bucketSize - 1;
    }

    public void clear() {
        Arrays.fill(this.buckets, null);
    }

    public T get(int index) {
        T[] bucket = this.buckets[index >> this.indexFactor];

        if (bucket == null) {
            return null;
        }

        return bucket[index & this.indexMask];
    }

    @SuppressWarnings("unchecked")
    public void put(int index, T value)  {
        int bucketIndex = index >> this.indexFactor;
        T[] bucket = this.buckets[bucketIndex];

        if (bucket == null) {
            this.buckets[bucketIndex] = (bucket = (T[]) new Object[this.bucketSize]);
        }

        bucket[index & this.indexMask] = value;
    }
}
