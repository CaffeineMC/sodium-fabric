package me.jellysquid.mods.sodium.common.util.collections;

import it.unimi.dsi.fastutil.PriorityQueue;
import me.jellysquid.mods.sodium.client.SodiumClientMod;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

public class FutureQueueDrainingIterator<T> implements Iterator<T> {
    private final PriorityQueue<CompletableFuture<T>> queue;
    private T next = null;

    public FutureQueueDrainingIterator(PriorityQueue<CompletableFuture<T>> queue) {
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
            CompletableFuture<T> future = queue.dequeue();

            try {
                next = future.join();
                return;
            } catch (CancellationException e) {
                SodiumClientMod.logger().warn("Future was cancelled: {}", future);
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
