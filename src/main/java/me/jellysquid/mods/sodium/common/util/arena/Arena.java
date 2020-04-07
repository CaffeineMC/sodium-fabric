package me.jellysquid.mods.sodium.common.util.arena;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Supplier;

public class Arena<T extends ReusableObject> {
    private final ArrayBlockingQueue<T> pool;
    private final Supplier<T> factory;

    public Arena(int size, Supplier<T> factory) {
        this.pool = new ArrayBlockingQueue<>(size);
        this.factory = factory;
    }

    public void reset() {
        this.pool.clear();
    }

    public T allocate() {
        T obj = this.pool.poll();

        if (obj == null) {
            obj = this.factory.get();
        }

        return obj;
    }

    public void reclaim(T obj) {
        if (this.pool.offer(obj)) {
            obj.reset();
        }
    }
}
