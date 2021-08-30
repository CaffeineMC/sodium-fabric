package me.jellysquid.mods.sodium.client.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.lang.reflect.Array;
import java.util.Arrays;

public class UnicodeCodepointTable<T> {
    // ASCII character table
    private static final int ARRAY_SIZE = 256;

    private final T[] array;
    private final Int2ObjectMap<T> hash = new Int2ObjectOpenHashMap<>();

    @SuppressWarnings("unchecked")
    public UnicodeCodepointTable(Class<T> type) {
        this.array = (T[]) Array.newInstance(type, ARRAY_SIZE);
    }

    public void clear() {
        Arrays.fill(this.array, null);
    }

    public T get(int index) {
        if (index < this.array.length) {
            return this.array[index];
        } else {
            return this.getHashed(index);
        }
    }

    private T getHashed(int index) {
        return this.hash.get(index);
    }

    public void put(int index, T value)  {
        if (index < this.array.length) {
            this.array[index] = value;
        } else {
            this.putHashed(index, value);
        }
    }

    private void putHashed(int index, T value) {
        this.hash.put(index, value);
    }
}
