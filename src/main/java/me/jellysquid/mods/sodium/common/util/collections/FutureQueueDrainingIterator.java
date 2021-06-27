package me.jellysquid.mods.sodium.common.util.collections;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

public class FutureQueueDrainingIterator<T> implements Iterator<T> {
    private final Queue<CompletableFuture<T>> queue;

    public FutureQueueDrainingIterator(Queue<CompletableFuture<T>> queue) {
        this.queue = queue;
    }

    @Override
    public boolean hasNext() {
        return !this.queue.isEmpty();
    }

    @Override
    public T next() {
        return this.queue.remove().join();
    }
}
