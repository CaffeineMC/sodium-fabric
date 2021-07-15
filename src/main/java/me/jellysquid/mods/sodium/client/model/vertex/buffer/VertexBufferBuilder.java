package me.jellysquid.mods.sodium.client.model.vertex.buffer;

import me.jellysquid.mods.sodium.client.gl.attribute.BufferVertexFormat;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class VertexBufferBuilder implements VertexBufferView {
    private final BufferVertexFormat vertexFormat;
    private final int initialCapacity;

    private ByteBuffer buffer;
    private int writerOffset;
    private int count;
    private int capacity;

    public VertexBufferBuilder(BufferVertexFormat vertexFormat, int initialCapacity) {
        this.vertexFormat = vertexFormat;

        this.buffer = null;
        this.capacity = initialCapacity;
        this.writerOffset = 0;
        this.initialCapacity = initialCapacity;
    }

    private void grow(int len) {
        // The new capacity will at least as large as the write it needs to service
        int cap = Math.max(this.capacity * 2, this.capacity + len);

        // Update the buffer and capacity now
        this.setBufferSize(cap);
    }

    private void setBufferSize(int cap) {
        this.buffer = MemoryUtil.memRealloc(this.buffer, cap);
        this.capacity = cap;
    }

    @Override
    public boolean ensureBufferCapacity(int bytes) {
        if (this.writerOffset + bytes <= this.capacity) {
            return false;
        }

        this.grow(bytes);

        return true;
    }

    @Override
    public ByteBuffer getDirectBuffer() {
        return this.buffer;
    }

    @Override
    public int getWriterPosition() {
        return this.writerOffset;
    }

    @Override
    public void flush(int vertexCount, BufferVertexFormat format) {
        if (this.vertexFormat != format) {
            throw new IllegalStateException("Mis-matched vertex format (expected: [" + format + "], currently using: [" + this.vertexFormat + "])");
        }

        this.count += vertexCount;
        this.writerOffset = this.count * format.getStride();
    }

    @Override
    public BufferVertexFormat getVertexFormat() {
        return this.vertexFormat;
    }

    public boolean isEmpty() {
        return this.count <= 0;
    }

    public int getCount() {
        return this.count;
    }

    public void start() {
        this.writerOffset = 0;
        this.count = 0;

        this.setBufferSize(this.initialCapacity);
    }

    public void destroy() {
        if (this.buffer != null) {
            MemoryUtil.memFree(this.buffer);
        }

        this.buffer = null;
    }

    public NativeBuffer pop() {
        if (this.writerOffset == 0) {
            return null;
        }

        return NativeBuffer.copy(MemoryUtil.memByteBuffer(MemoryUtil.memAddress(this.buffer), this.writerOffset));
    }
}
