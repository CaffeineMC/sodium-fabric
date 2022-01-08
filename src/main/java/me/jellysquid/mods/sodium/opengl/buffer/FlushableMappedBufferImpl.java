package me.jellysquid.mods.sodium.opengl.buffer;

import org.lwjgl.opengl.GL45C;

import java.nio.ByteBuffer;

public class FlushableMappedBufferImpl extends MappedBufferImpl implements FlushableMappedBuffer {

    public FlushableMappedBufferImpl(long capacity, int handle, ByteBuffer pointer) {
        super(capacity, handle, pointer);
    }

    @Override
    public void flush(long offset, long length) {
        GL45C.glFlushMappedNamedBufferRange(this.handle(), offset, length);
    }
}
