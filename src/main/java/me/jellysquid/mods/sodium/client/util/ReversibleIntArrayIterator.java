package me.jellysquid.mods.sodium.client.util;

import it.unimi.dsi.fastutil.ints.IntArrayList;

public class ReversibleIntArrayIterator {
    private final int[] elements;

    private final int step;

    private int cur;
    private int rem;

    public ReversibleIntArrayIterator(IntArrayList list, boolean reverse) {
        this(list.elements(), 0, list.size(), reverse);
    }

    public ReversibleIntArrayIterator(int[] elements, int start, int end, boolean reverse) {
        this.elements = elements;
        this.rem = end - start;

        this.step = reverse ? -1 : 1;
        this.cur = reverse ? end - 1 : start;
    }

    public boolean hasNext() {
        return this.rem > 0;
    }

    public int next() {
        int result = this.elements[this.cur];

        this.cur += this.step;
        this.rem--;

        return result;
    }
}