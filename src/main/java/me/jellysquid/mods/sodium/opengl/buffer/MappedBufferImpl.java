package me.jellysquid.mods.sodium.opengl.buffer;

import me.jellysquid.mods.sodium.opengl.util.EnumBitField;
import org.apache.commons.lang3.Validate;
import org.lwjgl.opengl.GL45C;

import java.nio.ByteBuffer;

public class MappedBufferImpl extends BufferImpl implements MappedBuffer {
    private final ByteBuffer data;
    private final EnumBitField<BufferMapFlags> flags;

    public MappedBufferImpl(long capacity, int handle, ByteBuffer data, EnumBitField<BufferMapFlags> flags) {
        super(capacity, handle);

        this.data = data;
        this.flags = flags;
    }

    @Override
    public void write(int writeOffset, ByteBuffer data) {
        this.data.put(writeOffset, data, 0, data.remaining());
    }

    @Override
    public void flush(int pos, int length) {
        Validate.isTrue(this.flags.contains(BufferMapFlags.EXPLICIT_FLUSH),
                "Buffer must be mapped with explicit flushing enabled");

        GL45C.glFlushMappedNamedBufferRange(this.handle(), pos, length);
    }

    @Override
    public ByteBuffer getView() {
        return this.data;
    }
}
