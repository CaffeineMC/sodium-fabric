package me.jellysquid.mods.sodium.client.util;

public class IntStack {
    private int[] array;
    private int idx;

    public IntStack(int size) {
        this.array = new int[size];
        this.idx = 0;
    }

    public void enqueue(int i) {
        if (this.idx >= this.array.length) {
            this.resize(this.array.length);
        }

        this.array[this.idx++] = i;
    }

    public int dequeue() {
        return this.array[--this.idx];
    }

    private void resize(int prevSize) {
        int[] old = this.array;
        this.array = new int[prevSize * 2];

        System.arraycopy(old, 0, this.array, 0, prevSize);
    }

    public boolean isEmpty() {
        return this.idx <= 0;
    }
}
