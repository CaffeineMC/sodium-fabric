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

        this.data = !flags.contains(BufferMapFlags.WRITE) ? data.asReadOnlyBuffer() : data;
        this.flags = flags;
    }

    @Override
    public void flush(long pos, long length) {
        if (this.flags.contains(BufferMapFlags.EXPLICIT_FLUSH)) {
            GL45C.glFlushMappedNamedBufferRange(this.handle(), pos, length);
        }
    }

    @Override
    public ByteBuffer view() {
        return this.data;
    }
}
