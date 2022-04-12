package net.caffeinemc.gfx.opengl.buffer;

import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.opengl.GlObject;

public class GlAbstractBuffer extends GlObject implements Buffer {
    private final long capacity;

    public GlAbstractBuffer(int handle, long capacity) {
        this.setHandle(handle);
        this.capacity = capacity;
    }

    public static int handle(Buffer buffer) {
        return ((GlAbstractBuffer) buffer).handle();
    }

    @Override
    public long capacity() {
        return this.capacity;
    }
}
