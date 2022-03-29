package net.caffeinemc.gfx.opengl.buffer;

import net.caffeinemc.gfx.api.buffer.AllocatedBuffer;
import net.caffeinemc.gfx.opengl.GlObject;

import java.nio.ByteBuffer;

public class GlAllocatedBuffer extends GlObject implements AllocatedBuffer {
    private final long capacity;
    private final ByteBuffer view;

    public GlAllocatedBuffer(ByteBuffer view, long capacity, int handle) {
        this.view = view;
        this.capacity = capacity;
        this.setHandle(handle);
    }

    @Override
    public long capacity() {
        return this.capacity;
    }

    @Override
    public ByteBuffer view() {
        return this.view;
    }
}
