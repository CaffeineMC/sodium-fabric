package me.jellysquid.mods.sodium.client.gl.util;

public class MemoryTracker {
    private int allocated, used;

    public void onMemoryAllocate(int mem) {
        this.allocated += mem;
    }

    public void onMemoryRelease(int mem) {
        this.allocated -= mem;
    }

    public void onMemoryUse(int mem) {
        this.used += mem;
    }

    public void onMemoryFree(int mem) {
        this.used -= mem;
    }

    public int getUsedMemory() {
        return this.used;
    }

    public int getAllocatedMemory() {
        return this.allocated;
    }

    public void onMemoryAllocateAndUse(int mem) {
        this.onMemoryAllocate(mem);
        this.onMemoryUse(mem);
    }

    public void onMemoryFreeAndRelease(int mem) {
        this.onMemoryFree(mem);
        this.onMemoryRelease(mem);
    }
}
