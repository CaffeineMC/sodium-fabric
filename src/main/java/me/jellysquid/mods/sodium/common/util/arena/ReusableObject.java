package me.jellysquid.mods.sodium.common.util.arena;

public abstract class ReusableObject {
    private int refCount;

    public void allocateReference() {
        this.refCount++;
    }

    public void releaseReference() {
        this.refCount--;
    }

    public abstract void reset();

    public boolean hasReferences() {
        return this.refCount <= 0;
    }
}
