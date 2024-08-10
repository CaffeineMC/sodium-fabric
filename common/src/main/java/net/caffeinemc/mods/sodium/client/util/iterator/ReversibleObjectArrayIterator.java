package net.caffeinemc.mods.sodium.client.util.iterator;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ReversibleObjectArrayIterator<T> implements Iterator<T> {
    private final T[] array;
    private final int direction;

    private int currentIndex;
    private int remaining;

    public ReversibleObjectArrayIterator(ObjectArrayList<T> list, boolean reverse) {
        this(list.elements(), 0, list.size(), reverse);
    }

    public ReversibleObjectArrayIterator(T[] array, int start, int end, boolean reverse) {
        this.array = array;
        this.remaining = end - start;

        this.direction = reverse ? -1 : 1;
        this.currentIndex = reverse ? end - 1 : start;
    }

    public boolean hasNext() {
        return this.remaining > 0;
    }

    public T next() {
        if (!this.hasNext()) {
            throw new NoSuchElementException();
        }

        T result = this.array[this.currentIndex];

        this.currentIndex += this.direction;
        this.remaining--;

        return result;
    }
}
