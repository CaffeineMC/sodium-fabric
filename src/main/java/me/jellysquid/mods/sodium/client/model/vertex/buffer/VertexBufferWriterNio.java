package me.jellysquid.mods.sodium.client.model.vertex.buffer;

import me.jellysquid.mods.sodium.client.model.vertex.type.BufferVertexType;

import java.nio.ByteBuffer;

/**
 * A safe {@link VertexBufferWriter} implementation which uses Java's NIO library to write into memory buffers. All
 * write operations are checked and will throw an exception if an invalid memory access is detected. Supported on all
 * platforms.
 */
public abstract class VertexBufferWriterNio extends VertexBufferWriter {
    protected ByteBuffer byteBuffer;
    protected int writeOffset;

    protected VertexBufferWriterNio(VertexBufferView backingBuffer, BufferVertexType<?> vertexType) {
        super(backingBuffer, vertexType);
    }

    @Override
    protected void onBufferStorageChanged() {
        this.byteBuffer = this.backingBuffer.getDirectBuffer();
        this.writeOffset = this.backingBuffer.getWriterPosition();
    }

    @Override
    protected void advance() {
        this.writeOffset += this.vertexStride;

        super.advance();
    }
}
