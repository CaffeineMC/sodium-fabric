package me.jellysquid.mods.sodium.client.render.chunk.format.sfp;

import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferWriterNio;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkModelVertexFormats;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;

import java.nio.ByteBuffer;

public class ModelVertexBufferWriterNio extends VertexBufferWriterNio implements ModelVertexSink {
    public ModelVertexBufferWriterNio(VertexBufferView backingBuffer) {
        super(backingBuffer, ChunkModelVertexFormats.DEFAULT);
    }

    @Override
    public void writeVertex(int offsetX, int offsetY, int offsetZ, float posX, float posY, float posZ, int color, float u, float v, int light) {
        int i = this.writeOffset;

        ByteBuffer buffer = this.byteBuffer;
        buffer.put(i, (byte) offsetX);
        buffer.put(i + 1, (byte) offsetY);
        buffer.put(i + 2, (byte) offsetZ);

        buffer.putShort(i + 4, ModelVertexType.encodePosition(posX));
        buffer.putShort(i + 6, ModelVertexType.encodePosition(posY));
        buffer.putShort(i + 8, ModelVertexType.encodePosition(posZ));

        buffer.putInt(i + 12, color);

        buffer.putShort(i + 16, ModelVertexType.encodeBlockTexture(u));
        buffer.putShort(i + 18, ModelVertexType.encodeBlockTexture(v));

        buffer.putInt(i + 20, ModelVertexType.encodeLightMapTexCoord(light));

        this.advance();
    }
}
