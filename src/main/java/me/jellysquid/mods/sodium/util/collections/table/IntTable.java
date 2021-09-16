package me.jellysquid.mods.sodium.util.collections.table;

import java.util.Arrays;

public class IntTable extends AbstractTable {
    private int[] array;

    public IntTable() {
        this.array = new int[512];
    }

    public int create() {
        int id = this.getNextId();

        if (this.array.length <= id) {
            this.array = Arrays.copyOf(this.array, Math.max(this.array.length * 2, id));
        }

        return id;
    }

    public void remove(int index) {
        this.array[index] = 0;

        this.freeId(index);
    }

    public int get(int id) {
        return this.array[id];
    }

    public int size() {
        return this.array.length;
    }
}
