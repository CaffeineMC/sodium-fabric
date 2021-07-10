package me.jellysquid.mods.sodium.common.util.collections;

import java.util.Iterator;
import java.util.Queue;

public class QueueDrainingIterator<T> implements Iterator<T> {
    private final Queue<T> queue;

    public QueueDrainingIterator(Queue<T> queue) {
        this.queue = queue;
    }

    @Override
    public boolean hasNext() {
        return !this.queue.isEmpty();
    }

    @Override
    public T next() {
        return this.queue.remove();
    }
}
