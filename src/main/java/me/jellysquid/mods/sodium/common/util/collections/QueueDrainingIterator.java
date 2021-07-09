package me.jellysquid.mods.sodium.common.util.collections;

import it.unimi.dsi.fastutil.PriorityQueue;

import java.util.Iterator;

public class QueueDrainingIterator<T> implements Iterator<T> {
    private final PriorityQueue<T> queue;

    public QueueDrainingIterator(PriorityQueue<T> queue) {
        this.queue = queue;
    }

    @Override
    public boolean hasNext() {
        return !this.queue.isEmpty();
    }

    @Override
    public T next() {
        return this.queue.dequeue();
    }
}
