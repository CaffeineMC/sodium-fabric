package me.jellysquid.mods.sodium.client.render.chunk.format;

import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferWriterNio;

import java.nio.ByteBuffer;

public class ModelVertexBufferWriterNio extends VertexBufferWriterNio implements ModelVertexSink {
    public ModelVertexBufferWriterNio(VertexBufferView backingBuffer) {
        super(backingBuffer, ModelVertexType.INSTANCE);
    }

    @Override
    public void writeVertex(float posX, float posY, float posZ, int color, float u, float v, int light, int chunkId, int bits) {
        int i = this.writeOffset;

        ByteBuffer buffer = this.byteBuffer;
        buffer.putShort(i + 0, ModelVertexType.encodePosition(posX));
        buffer.putShort(i + 2, ModelVertexType.encodePosition(posY));
        buffer.putShort(i + 4, ModelVertexType.encodePosition(posZ));
        buffer.putShort(i + 6, (short) chunkId);

        buffer.putInt(i + 8, color);

        buffer.putShort(i + 12, ModelVertexType.encodeBlockTexture(u));
        buffer.putShort(i + 14, ModelVertexType.encodeBlockTexture(v));

        buffer.putInt(i + 16, ModelVertexType.encodeLightMapTexCoord(light));
        buffer.putInt(i + 20, bits);

        this.advance();
    }
}
