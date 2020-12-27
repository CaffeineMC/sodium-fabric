package me.jellysquid.mods.sodium.client.model.vertex.formats.quad.writer;

import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferWriterUnsafe;
import me.jellysquid.mods.sodium.client.model.vertex.formats.quad.QuadVertexSink;

public class QuadVertexBufferWriterUnsafe extends VertexBufferWriterUnsafe implements QuadVertexSink {
    public QuadVertexBufferWriterUnsafe(VertexBufferView backingBuffer) {
        super(backingBuffer, QuadVertexSink.VERTEX_FORMAT);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public void writeQuad(float x, float y, float z, int color, float u, float v, int light, int overlay, int normal) {
        long i = this.writePointer;

        UNSAFE.putFloat(i, x);
        UNSAFE.putFloat(i + 4, y);
        UNSAFE.putFloat(i + 8, z);
        UNSAFE.putInt(i + 12, color);
        UNSAFE.putFloat(i + 16, u);
        UNSAFE.putFloat(i + 20, v);
        UNSAFE.putInt(i + 24, overlay);
        UNSAFE.putInt(i + 28, light);
        UNSAFE.putInt(i + 32, normal);

        this.advance();
    }
}
