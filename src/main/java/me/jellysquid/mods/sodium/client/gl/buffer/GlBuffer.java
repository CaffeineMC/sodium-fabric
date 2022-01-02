package me.jellysquid.mods.sodium.client.gl.buffer;

import me.jellysquid.mods.sodium.client.gl.GlObject;

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
