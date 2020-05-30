package me.jellysquid.mods.sodium.common.util.pool;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class ReusableObject {
    /** The number of active references to this object **/
    private final AtomicInteger refCount = new AtomicInteger(0);

    /**
     * Acquires a reference for this object. This will prevent the object from being re-used until all references have
     * been released using {@link ReusableObject#releaseReference()}.
     */
    final void acquireReference() {
        int count = this.refCount.getAndIncrement();

        if (count <= 0) {
            throw new IllegalStateException("Reference cannot be acquired after all others have been dropped");
        }
    }

    /**
     * Releases a reference for this object and performs finalizers as necessary. If no references are held, this
     * will throw an exception.
     */
    final boolean releaseReference() {
        int count = this.refCount.decrementAndGet();

        if (count < 0) {
            throw new IllegalStateException("No references are currently held");
        }

        return count == 0;
    }

    /**
     * Called when the last reference is released. The implementation should release all resources of their own as to not
     * cause memory leaks when this object is added back to an pool.
     */
    protected abstract void reset();

    public void acquireOwner() {
        if (!this.refCount.compareAndSet(0, 1)) {
            throw new IllegalStateException("Object in pool still has references");
        }
    }
}
