package me.jellysquid.mods.sodium.common.util.arena;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class ReusableObject {
    /** The number of active references to this object **/
    private final AtomicInteger refCount = new AtomicInteger(0);

    /**
     * Acquires a reference for this object. This will prevent the object from being re-used until all references have
     * been released using {@link ReusableObject#releaseReference()}.
     */
    final void acquireReference() {
        this.refCount.getAndIncrement();
    }

    /**
     * Releases a reference for this object and performs finalizers as necessary. If no references are held, this
     * will throw an exception.
     */
    final boolean releaseReference() {
        if (!this.hasReferences()) {
            throw new IllegalStateException("No references are allocated");
        }

        boolean flag = this.refCount.getAndDecrement() <= 0;

        if (flag) {
            this.reset();
        }

        return flag;
    }

    /**
     * Called when the last reference is released. The implementation should release all resources of their own as to not
     * cause memory leaks when this object is added back to an arena.
     */
    protected abstract void reset();

    /**
     * @return True if the object has at least one reference, otherwise false
     */
    final boolean hasReferences() {
        return this.refCount.get() > 0;
    }
}
