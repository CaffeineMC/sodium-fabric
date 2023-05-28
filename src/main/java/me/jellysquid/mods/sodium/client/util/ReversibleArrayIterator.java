package me.jellysquid.mods.sodium.client.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ReversibleArrayIterator<T> {

    private final T[] array;

    private final int direction;

    private int currentIndex;

    private int remaining;

    public ReversibleArrayIterator(ObjectArrayList<T> list, boolean reverse) {
        this(list.elements(), 0, list.size(), reverse);
    }

    public ReversibleArrayIterator(T[] array, int start, int end, boolean reverse) {
        this.array = array;
        this.remaining = end - start;
        this.direction = reverse ? -1 : 1;
        this.currentIndex = reverse ? end - 1 : start;
    }

    public T next() {
        T result = null;
        if (this.remaining > 0) {
            result = this.array[this.currentIndex];
            this.currentIndex += this.direction;
            this.remaining--;
        }
        return result;
    }
}
