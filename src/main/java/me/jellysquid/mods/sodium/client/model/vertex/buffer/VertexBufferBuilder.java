package me.jellysquid.mods.sodium.client.model.vertex.buffer;

import me.jellysquid.mods.sodium.client.gl.attribute.BufferVertexFormat;
import net.minecraft.client.util.GlAllocationUtils;

import java.nio.ByteBuffer;

public class VertexBufferBuilder implements VertexBufferView {
    private final BufferVertexFormat vertexFormat;

    private ByteBuffer buffer;
    private int writerOffset;
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

        this.writerOffset += vertexCount * format.getStride();
    }

    @Override
    public BufferVertexFormat getVertexFormat() {
        return this.vertexFormat;
    }

    public boolean isEmpty() {
        return this.writerOffset == 0;
    }

    public int getSize() {
        return this.writerOffset;
    }

    /**
     * Ends the stream of written data and makes a copy of it to be passed around.
     */
    public void copyInto(ByteBuffer dst) {
        // Mark the slice of memory that needs to be copied
        this.buffer.position(0);
        this.buffer.limit(this.writerOffset);

        // Allocate a new buffer which is just large enough to contain the slice of vertex data
        // The buffer is then flipped after the operation so the callee sees a range of bytes from (0,len] which can
        // then be immediately passed to native libraries or the graphics driver
        dst.put(this.buffer.slice());

        // Reset the position and limit set earlier of the backing scratch buffer
        this.buffer.clear();
        this.writerOffset = 0;
    }
}
