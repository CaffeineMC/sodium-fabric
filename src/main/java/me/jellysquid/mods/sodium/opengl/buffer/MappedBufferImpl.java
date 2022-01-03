package me.jellysquid.mods.sodium.opengl.buffer;

import java.nio.ByteBuffer;

public class MappedBufferImpl extends BufferImpl implements MappedBuffer {
    private final ByteBuffer data;

    public MappedBufferImpl(long capacity, int handle, ByteBuffer data) {
        super(capacity, handle);

        this.data = data;
    }

    @Override
    public void write(ByteBuffer data, int writeOffset) {
        this.data.put(writeOffset, data, 0, data.remaining());
    }
}
