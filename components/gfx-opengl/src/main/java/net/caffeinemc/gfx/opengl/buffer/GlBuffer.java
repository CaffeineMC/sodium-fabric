package net.caffeinemc.gfx.opengl.buffer;

import net.caffeinemc.gfx.opengl.GlObject;
import net.caffeinemc.gfx.api.buffer.Buffer;

public class GlBuffer extends GlObject implements Buffer {
    private final long capacity;

    public GlBuffer(long capacity, int handle) {
        this.setHandle(handle);

        this.capacity = capacity;
    }

    @Override
    public long getCapacity() {
        return this.capacity;
    }
}
