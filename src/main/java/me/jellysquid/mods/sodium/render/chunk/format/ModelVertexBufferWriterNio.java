package me.jellysquid.mods.sodium.render.chunk.format;

import me.jellysquid.mods.sodium.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.model.vertex.buffer.VertexBufferWriterNio;

import java.nio.ByteBuffer;

public class ModelVertexBufferWriterNio extends VertexBufferWriterNio implements ModelVertexSink {
    public ModelVertexBufferWriterNio(VertexBufferView backingBuffer) {
        super(backingBuffer, ModelVertexType.INSTANCE);
    }

    @Override
    public void writeVertex(long position, int color, int blockTexture, int lightTexture, short chunkIndex, int materialBits) {
        int i = this.writeOffset;

        ByteBuffer buf = this.byteBuffer;
        buf.putLong(i, position);
        buf.putShort(i + 6, chunkIndex);
        buf.putInt(i + 8, color);
        buf.putInt(i + 12, blockTexture);
        buf.putInt(i + 16, lightTexture);
        buf.putInt(i + 20, materialBits);

        this.advance();
    }
}
