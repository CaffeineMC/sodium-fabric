package me.jellysquid.mods.sodium.common.util.collections;

import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

public class FutureDequeDrain<T> implements Iterator<T> {
    private final Deque<CompletableFuture<T>> deque;

    public FutureDequeDrain(Deque<CompletableFuture<T>> deque) {
        this.deque = deque;
    }

    @Override
    public boolean hasNext() {
        return !this.deque.isEmpty();
    }

    @Override
    public T next() {
        return this.deque.remove().join();
    }
}
