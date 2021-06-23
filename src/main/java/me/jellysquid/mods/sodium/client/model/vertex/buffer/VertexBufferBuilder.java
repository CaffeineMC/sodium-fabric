package me.jellysquid.mods.sodium.client.model.vertex.buffer;

import me.jellysquid.mods.sodium.client.gl.attribute.BufferVertexFormat;
import net.minecraft.client.util.GlAllocationUtils;

import java.nio.ByteBuffer;

public class VertexBufferBuilder implements VertexBufferView {
    private final BufferVertexFormat vertexFormat;

    private ByteBuffer buffer;
    private int writerOffset;
    private int count;
    private int capacity;

    public VertexBufferBuilder(BufferVertexFormat vertexFormat, int initialCapacity) {
        this.vertexFormat = vertexFormat;

        this.buffer = GlAllocationUtils.allocateByteBuffer(initialCapacity);
        this.capacity = initialCapacity;
        this.writerOffset = 0;
    }

    private void grow(int len) {
        // The new capacity will at least as large as the write it needs to service
        int cap = Math.max(this.capacity * 2, this.capacity + len);

        // Allocate a new buffer and copy the old buffer's contents into it
        ByteBuffer buffer = GlAllocationUtils.allocateByteBuffer(cap);
        buffer.put(this.buffer);
        buffer.position(0);

        // Update the buffer and capacity now
        this.buffer = buffer;
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

    public int getByteSize() {
        return this.writerOffset;
    }

    public int getCount() {
        return this.count;
    }

    /**
     * Ends the stream of written data and makes a copy of it to be passed around.
     */
    public void get(ByteBuffer dst) {
        this.buffer.clear();
        this.buffer.limit(this.writerOffset);

        dst.put(this.buffer);

        this.buffer.clear();
    }

    public void reset() {
        this.writerOffset = 0;
        this.count = 0;
    }
}
