package me.jellysquid.mods.sodium.common.util.pool;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Supplier;

/**
 * A fixed-size object pool which allows expensive allocations to be tracked and re-used. The pool will use the
 * provided factory to instantiate new objects in order to service requests when no objects are available in the pool.
 *
 * Reference counting is used internally in order to prevent difficult to debug issues/ When all references are dropped
 * to a {@link ReusableObject}, it can be added back to a pool in order to be re-used by future requests. If references
 * are still held by other code, trying to add the object to the pool will result in an exception being thrown.
 *
 * This class is thread-safe and can service requests from multiple threads concurrently. However, the objects serviced
 * by the pool are not necessarily thread safe.
 *
 * @param <T> The type of object which will be serviced by the pool
 */
public class ObjectPool<T extends ReusableObject> {
    private final ArrayBlockingQueue<T> pool;
    private final Supplier<T> factory;

    /**
     * @param size The maximum number of objects to keep in memory for re-use
     * @param factory The factory which will be called to instantiate new objects when the pool is empty
     */
    public ObjectPool(int size, Supplier<T> factory) {
        this.pool = new ArrayBlockingQueue<>(size);
        this.factory = factory;
    }

    /**
     * Drops all objects from the pool.
     */
    public void reset() {
        synchronized (this.pool) {
            this.pool.clear();
        }
    }

    /**
     * Allocates an object from the pool and acquires a reference for it to be handed to the caller. If there are
     * re-usable objects in the pool, they will be used instead of allocating a new object.
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
     * Releases a reference to the object and adds it back into the pool if no references are held. This method passes
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
     * {@link ObjectPool#release(ReusableObject)} is called.
     */
    public void acquireReference(T obj) {
        obj.acquireReference();
    }
}
