package me.jellysquid.mods.sodium.opengl.buffer;

import me.jellysquid.mods.sodium.opengl.GlObject;

public class GlBuffer extends GlObject {
    private final long capacity;

    public GlBuffer(long capacity, int handle) {
        this.setHandle(handle);

        this.capacity = capacity;
    }

    public long getCapacity() {
        return this.capacity;
    }
}
