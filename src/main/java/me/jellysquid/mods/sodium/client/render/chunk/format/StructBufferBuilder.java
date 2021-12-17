package me.jellysquid.mods.sodium.client.render.chunk.format;

import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import org.apache.commons.lang3.Validate;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class StructBufferBuilder {
    private ByteBuffer buffer;

    @Deprecated(forRemoval = true)
    private final int stride;

    public StructBufferBuilder(int initialCapacity, int stride) {
        this.buffer = MemoryUtil.memAlloc(initialCapacity * stride);
        this.stride = stride;
    }

    public void add(ByteBuffer slice) {
        Validate.isTrue(slice.remaining() % this.stride == 0);

        if (this.buffer.remaining() < slice.remaining()) {
            this.reallocate(slice.remaining());
        }

        this.buffer.put(slice.asReadOnlyBuffer());
    }

    private void reallocate(int bytes) {
        this.buffer = MemoryUtil.memRealloc(this.buffer, Math.max(this.buffer.capacity() + bytes, this.buffer.capacity() * 2));
    }

    public void destroy() {
        MemoryUtil.memFree(this.buffer);
        this.buffer = null;
    }

    public void reset() {
        this.buffer.clear();
    }

    public ByteBuffer window() {
        return this.buffer.slice(0, this.buffer.position());
    }
}
