package net.caffeinemc.sodium.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;

import java.util.List;
import java.util.function.Supplier;

public class Pool<T> {
    private final Supplier<T> factory;
    private final ObjectArrayFIFOQueue<T> queue;

    public Pool(Supplier<T> factory) {
        this.factory = factory;
        this.queue = new ObjectArrayFIFOQueue<>();
    }

    public T acquire() {
        if (!this.queue.isEmpty()) {
            return this.queue.dequeue();
        }

        return this.factory.get();
    }

    public void release(List<T> list) {
        for (var obj : list) {
            this.queue.enqueue(obj);
        }

        list.clear();
    }
}
