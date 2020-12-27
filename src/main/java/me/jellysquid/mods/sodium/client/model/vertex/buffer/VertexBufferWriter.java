package me.jellysquid.mods.sodium.client.model.vertex.buffer;

import me.jellysquid.mods.sodium.client.model.vertex.VertexSink;
import net.minecraft.client.render.VertexFormat;

public abstract class VertexBufferWriter implements VertexSink {
    protected final VertexBufferView backingBuffer;

    protected final VertexFormat vertexFormat;
    protected final int vertexStride;

    protected int vertexCount;

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

    protected abstract void onBufferStorageChanged();
}
