package me.jellysquid.mods.thingl.buffer;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class BufferMappingImpl implements BufferMapping {
    private final BufferImpl buffer;
    private final ByteBuffer map;

    protected boolean disposed;

    public BufferMappingImpl(BufferImpl buffer, ByteBuffer map) {
        this.buffer = buffer;
        this.map = map;
    }

    @Override
    public void write(ByteBuffer data, int writeOffset) {
        MemoryUtil.memCopy(MemoryUtil.memAddress(data), MemoryUtil.memAddress(this.map, writeOffset), data.remaining());
    }

    public BufferImpl getBufferObject() {
        return this.buffer;
    }

    public void dispose() {
        this.disposed = true;
    }

    public boolean isDisposed() {
        return this.disposed;
    }

    public ByteBuffer getMemoryBuffer() {
        return this.map;
    }

    public void checkDisposed() {
        if (this.isDisposed()) {
            throw new IllegalStateException("Buffer mapping is already disposed");
        }
    }
}
