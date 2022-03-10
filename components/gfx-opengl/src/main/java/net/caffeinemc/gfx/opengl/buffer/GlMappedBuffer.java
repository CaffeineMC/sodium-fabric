package net.caffeinemc.gfx.opengl.buffer;

import net.caffeinemc.gfx.api.buffer.BufferMapFlags;
import net.caffeinemc.gfx.api.buffer.MappedBuffer;
import org.apache.commons.lang3.Validate;
import org.lwjgl.opengl.GL45C;

import java.nio.ByteBuffer;
import java.util.Set;

public class GlMappedBuffer extends GlBuffer implements MappedBuffer {
    private final ByteBuffer data;
    private final Set<BufferMapFlags> flags;

    public GlMappedBuffer(long capacity, int handle, ByteBuffer data, Set<BufferMapFlags> flags) {
        super(capacity, handle);

        this.data = data;
        this.flags = flags;
    }

    @Override
    public void write(int writeOffset, ByteBuffer data) {
        this.data.put(writeOffset, data, 0, data.remaining());
    }

    @Override
    public void flush(long pos, long length) {
        Validate.isTrue(this.flags.contains(BufferMapFlags.EXPLICIT_FLUSH),
                "Buffer must be mapped with explicit flushing enabled");

        GL45C.glFlushMappedNamedBufferRange(this.handle(), pos, length);
    }

    @Override
    public ByteBuffer getView() {
        return this.data;
    }
}
