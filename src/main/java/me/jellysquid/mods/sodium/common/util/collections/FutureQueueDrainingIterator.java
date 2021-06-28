package me.jellysquid.mods.sodium.common.util.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

public class FutureQueueDrainingIterator<T> implements Iterator<T> {
    private final Queue<CompletableFuture<T>> queue;
    private T next = null;

    public FutureQueueDrainingIterator(Queue<CompletableFuture<T>> queue) {
        this.queue = queue;
    }

    @Override
    public boolean hasNext() {
        if (next != null) {
            return true;
        }

        findNext();

        return next != null;
    }

    private void findNext() {
        while (!queue.isEmpty()) {
            CompletableFuture<T> future = queue.remove();

            try {
                next = future.join();
                return;
            } catch (CancellationException e) {
                // no-op
            }
        }
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        T result = next;
        next = null;

        return result;
    }
}
