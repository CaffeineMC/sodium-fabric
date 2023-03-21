package me.jellysquid.mods.sodium.client.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;

import java.util.function.Consumer;

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

    public boolean hasNext() {
        return this.remaining > 0;
    }

    public T next() {
        T result = this.array[this.currentIndex];

        this.currentIndex += this.direction;
        this.remaining--;

        return result;
    }
}
