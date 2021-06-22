package me.jellysquid.mods.sodium.client.model.vertex.formats.line.writer;

import me.jellysquid.mods.sodium.client.model.vertex.VanillaVertexTypes;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferWriterNio;
import me.jellysquid.mods.sodium.client.model.vertex.formats.line.LineVertexSink;

import java.nio.ByteBuffer;

public class LineVertexBufferWriterNio extends VertexBufferWriterNio implements LineVertexSink {
    public LineVertexBufferWriterNio(VertexBufferView backingBuffer) {
        super(backingBuffer, VanillaVertexTypes.LINES);
    }

    @Override
    public void ensureCapacity(int count) {
        super.ensureCapacity(count * 2);
    }

    @Override
    public void vertexLine(float x, float y, float z, int color, int normal) {
        for (int i = 0; i < 2; i++) {
            this.vertexLine0(x, y, z, color, normal);
        }
    }

    private void vertexLine0(float x, float y, float z, int color, int normal) {
        int i = this.writeOffset;

        ByteBuffer buffer = this.byteBuffer;
        buffer.putFloat(i, x);
        buffer.putFloat(i + 4, y);
        buffer.putFloat(i + 8, z);
        buffer.putInt(i + 12, color);
        buffer.putInt(i + 16, normal);

        this.advance();
    }
}
