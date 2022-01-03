package me.jellysquid.mods.sodium.opengl.buffer;

import java.nio.ByteBuffer;

public class GlMappedBuffer extends GlBuffer {
    public final ByteBuffer data;

    public GlMappedBuffer(long capacity, int handle, ByteBuffer data) {
        super(capacity, handle);

        this.data = data;
    }

    public void write(ByteBuffer data, int writeOffset) {
        this.data.put(writeOffset, data, 0, data.remaining());
    }
}
