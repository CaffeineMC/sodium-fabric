package me.jellysquid.mods.sodium.client.model.vertex.formats.line.writer;

import me.jellysquid.mods.sodium.client.model.vertex.VanillaVertexTypes;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferWriterUnsafe;
import me.jellysquid.mods.sodium.client.model.vertex.formats.line.LineVertexSink;

public class LineVertexBufferWriterUnsafe extends VertexBufferWriterUnsafe implements LineVertexSink {
    public LineVertexBufferWriterUnsafe(VertexBufferView backingBuffer) {
        super(backingBuffer, VanillaVertexTypes.LINES);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public void vertexLine(float x, float y, float z, int color) {
        long i = this.writePointer;

        UNSAFE.putFloat(i, x);
        UNSAFE.putFloat(i + 4, y);
        UNSAFE.putFloat(i + 8, z);
        UNSAFE.putInt(i + 12, color);

        this.advance();
    }
}
