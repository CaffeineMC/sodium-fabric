package me.jellysquid.mods.sodium.client.model.vertex.buffer;

import net.minecraft.client.render.VertexFormat;

import java.nio.ByteBuffer;

public abstract class VertexBufferWriterNio extends VertexBufferWriter {
    protected ByteBuffer byteBuffer;
    protected int writeOffset;

    protected VertexBufferWriterNio(VertexBufferView backingBuffer, VertexFormat vertexFormat) {
        super(backingBuffer, vertexFormat);
    }

    @Override
    protected void onBufferStorageChanged() {
        this.byteBuffer = this.backingBuffer.getDirectBuffer();
        this.writeOffset = this.backingBuffer.getElementOffset();
    }

    protected void advance() {
        this.writeOffset += this.vertexStride;
        this.vertexCount++;
    }

}
