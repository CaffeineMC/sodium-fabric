package me.jellysquid.mods.sodium.client.gl.util;

public class MemoryTracker {
    private long allocated, used;

    public void onMemoryAllocate(long mem) {
        this.allocated += mem;
    }

    public void onMemoryRelease(long mem) {
        this.allocated -= mem;
    }

    public void onMemoryUse(long mem) {
        this.used += mem;
    }

    public void onMemoryFree(long mem) {
        this.used -= mem;
    }

    public long getUsedMemory() {
        return this.used;
    }

    public long getAllocatedMemory() {
        return this.allocated;
    }

    public void onMemoryAllocateAndUse(long mem) {
        this.onMemoryAllocate(mem);
        this.onMemoryUse(mem);
    }

    public void onMemoryFreeAndRelease(long mem) {
        this.onMemoryFree(mem);
        this.onMemoryRelease(mem);
    }
}
