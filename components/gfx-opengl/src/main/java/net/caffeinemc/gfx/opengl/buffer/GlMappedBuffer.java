package net.caffeinemc.gfx.opengl.buffer;

import net.caffeinemc.gfx.api.buffer.MappedBufferFlags;
import net.caffeinemc.gfx.api.buffer.MappedBuffer;
import net.caffeinemc.gfx.api.device.RenderConfiguration;
import org.apache.commons.lang3.Validate;
import org.lwjgl.opengl.GL45C;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Set;

public class GlMappedBuffer extends GlAbstractBuffer implements MappedBuffer {
    private final ByteBuffer view;
    private final Set<MappedBufferFlags> flags;

    public GlMappedBuffer(int handle, ByteBuffer view, Set<MappedBufferFlags> flags) {
        super(handle, view.capacity());

        this.view = view;
        this.flags = Collections.unmodifiableSet(flags);
    }

    @Override
    public void flush(long offset, long length) {
        if (RenderConfiguration.API_CHECKS) {
            Validate.isTrue(this.flags.contains(MappedBufferFlags.EXPLICIT_FLUSH), "Buffer is not mapped for explicit flushing");

            Validate.isTrue(offset >= 0, "The offset must be greater than or equal to zero");
            Validate.isTrue(offset + length <= this.capacity(), "Range is outside of buffer bounds");
        }

        GL45C.glFlushMappedNamedBufferRange(this.handle(), offset, length);
    }

    @Override
    public ByteBuffer view() {
        return this.view;
    }

    @Override
    public Set<MappedBufferFlags> flags() {
        return this.flags;
    }
}
