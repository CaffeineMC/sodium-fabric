package me.jellysquid.mods.sodium.client.model.vertex.buffer;

import me.jellysquid.mods.sodium.client.model.vertex.type.BufferVertexType;
import org.lwjgl.system.MemoryUtil;

/**
 * An unsafe {@link VertexBufferWriter} implementation which uses direct memory operations to enable fast blitting of
 * data into memory buffers. Only available on JVMs which support {@link sun.misc.Unsafe}, but generally produces much
 * better optimized code than other implementations. The implementation does not check for invalid memory accesses,
 * meaning that errors can corrupt process memory.
 */
public abstract class VertexBufferWriterUnsafe extends VertexBufferWriter {
    /**
     * The write pointer into the buffer storage. This is advanced by the vertex stride every time
     * {@link VertexBufferWriterUnsafe#advance()} is called.
     */
    protected long writePointer;

    protected VertexBufferWriterUnsafe(VertexBufferView backingBuffer, BufferVertexType<?> vertexType) {
        super(backingBuffer, vertexType);
    }

    @Override
    protected void onBufferStorageChanged() {
        this.writePointer = MemoryUtil.memAddress(this.backingBuffer.getDirectBuffer(), this.backingBuffer.getWriterPosition());
    }

    @Override
    protected void advance() {
        this.writePointer += this.vertexStride;

        super.advance();
    }
}
