package me.jellysquid.mods.sodium.util.collections.table;

import java.util.Arrays;
import java.util.function.IntFunction;

public class Table<T> extends AbstractTable {
    private T[] array;

    @SuppressWarnings("unchecked")
    public Table() {
        this.array = (T[]) new Object[512];
    }

    public int create(IntFunction<T> supplier) {
        int id = this.getNextId();

        if (this.array.length <= id) {
            this.array = Arrays.copyOf(this.array, Math.max(this.array.length * 2, id));
        }

        this.array[id] = supplier.apply(id);

        return id;
    }

    public void remove(int index) {
        this.array[index] = null;
        this.freeId(index);
    }

    public T get(int id) {
        return this.array[id];
    }

    public int size() {
        return this.array.length;
    }
}
