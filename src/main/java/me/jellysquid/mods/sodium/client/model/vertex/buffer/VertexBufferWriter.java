package me.jellysquid.mods.sodium.client.model.vertex.buffer;

import me.jellysquid.mods.sodium.client.model.vertex.VertexSink;
import net.minecraft.client.render.VertexFormat;

/**
 * Base implementation of a {@link VertexSink} which writes into a {@link VertexBufferView} directly.
 */
public abstract class VertexBufferWriter implements VertexSink {
    protected final VertexBufferView backingBuffer;

    protected final VertexFormat vertexFormat;
    protected final int vertexStride;

    private int vertexCount;

    protected VertexBufferWriter(VertexBufferView backingBuffer, VertexFormat vertexFormat) {
        if (backingBuffer.getVertexFormat() != vertexFormat) {
            throw new IllegalArgumentException("Backing buffer is using vertex format [" + backingBuffer.getVertexFormat() +
                    "] but this writer requires [" + vertexFormat + "]");
        }

        this.backingBuffer = backingBuffer;

        this.vertexFormat = vertexFormat;
        this.vertexStride = vertexFormat.getVertexSize();

        this.onBufferStorageChanged();
    }

    @Override
    public void ensureCapacity(int count) {
        if (this.backingBuffer.ensureBufferCapacity((this.vertexCount + count) * this.vertexStride)) {
            this.onBufferStorageChanged();
        }
    }

    @Override
    public void flush() {
        this.backingBuffer.flush(this.vertexCount, this.vertexFormat);
        this.vertexCount = 0;
    }

    /**
     * Advances the write pointer forward by the stride of one vertex. This should always be called after a
     * vertex is written. Implementations which override this should always call invoke the super implementation.
     */
    protected void advance() {
        this.vertexCount++;
    }

    /**
     * Called when the underlying memory buffer to the backing storage changes. When this is called, the implementation
     * should update any pointers
     */
    protected abstract void onBufferStorageChanged();
}
