package me.jellysquid.mods.thingl.buffer;

import me.jellysquid.mods.thingl.util.EnumBitField;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class BufferMappingImpl implements BufferMapping {
    private final BufferImpl buffer;
    private final ByteBuffer map;
    private final EnumBitField<BufferMapFlags> flags;

    protected boolean disposed;

    public BufferMappingImpl(BufferImpl buffer, ByteBuffer map, EnumBitField<BufferMapFlags> flags) {
        this.buffer = buffer;
        this.map = map;
        this.flags = flags;
    }

    @Override
    public ByteBuffer getPointer() {
        return this.map;
    }

    @Override
    public EnumBitField<BufferMapFlags> getFlags() {
        return this.flags;
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

    public void checkDisposed() {
        if (this.isDisposed()) {
            throw new IllegalStateException("Buffer mapping is already disposed");
        }
    }

}
