package me.jellysquid.mods.sodium.opengl.buffer;

import java.nio.ByteBuffer;

public class MappedBufferImpl extends BufferImpl implements MappedBuffer {
    private final ByteBuffer pointer;

    public MappedBufferImpl(long capacity, int handle, ByteBuffer pointer) {
        super(capacity, handle);

        this.pointer = pointer;
    }

    @Override
    public ByteBuffer getPointer() {
        return pointer;
    }

}
