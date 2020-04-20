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

    /**
     * Drops all objects from the arena.
     */
    public void reset() {
        synchronized (this.pool) {
            this.pool.clear();
        }
    }

    /**
     * Allocates an object from the arena and acquires a reference for it to be handed to the caller. If there are
     * re-usable objects in the arena, they will be used instead of allocating a new object.
     */
    public T allocate() {
        T obj;

        synchronized (this.pool) {
            obj = this.pool.poll();
        }

        if (obj == null) {
            obj = this.factory.get();
        }

        obj.acquireOwner();

        return obj;
    }

    /**
     * Releases a reference to the object and adds it back into the arena if no references are held. This method passes
     * ownership *away* from the caller. Using the object after this method returns is invalid.
     */
    public void release(T obj) {
        if (obj.releaseReference()) {
            obj.reset();

            synchronized (this.pool) {
                this.pool.offer(obj);
            }
        }
    }

    /**
     * Acquires a reference to the object. This prevents the object from being reclaimed until
     * {@link Arena#release(ReusableObject)} is called.
     */
    public void acquireReference(T obj) {
        obj.acquireReference();
    }
}
