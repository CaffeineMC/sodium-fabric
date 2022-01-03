package me.jellysquid.mods.sodium.opengl.buffer;

import me.jellysquid.mods.sodium.opengl.ManagedObject;

public class BufferImpl extends ManagedObject implements Buffer {
    private final long capacity;

    public BufferImpl(long capacity, int handle) {
        this.setHandle(handle);

        this.capacity = capacity;
    }

    @Override
    public long getCapacity() {
        return this.capacity;
    }
}
