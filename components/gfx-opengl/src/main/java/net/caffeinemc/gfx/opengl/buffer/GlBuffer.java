package net.caffeinemc.gfx.opengl.buffer;

import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.opengl.GlObject;

public class GlBuffer extends GlObject implements Buffer {
    private final long capacity;

    public GlBuffer(int handle, long capacity) {
        this.setHandle(handle);
        this.capacity = capacity;
    }

    @Override
    public long capacity() {
        return this.capacity;
    }

    public static int getHandle(Buffer buffer) {
        return ((GlBuffer) buffer).getHandle();
    }
}
