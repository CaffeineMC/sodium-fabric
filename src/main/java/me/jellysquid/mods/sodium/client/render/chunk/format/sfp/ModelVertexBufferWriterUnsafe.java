package me.jellysquid.mods.sodium.client.render.chunk.format.sfp;

import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferWriterUnsafe;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkModelVertexFormats;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import me.jellysquid.mods.sodium.client.util.Int10;

public class ModelVertexBufferWriterUnsafe extends VertexBufferWriterUnsafe implements ModelVertexSink {
    public ModelVertexBufferWriterUnsafe(VertexBufferView backingBuffer) {
        super(backingBuffer, ChunkModelVertexFormats.DEFAULT);
    }

    @Override
    public void writeVertex(int offsetX, int offsetY, int offsetZ, float posX, float posY, float posZ, int color, float u, float v, int light) {
        long i = this.writePointer;

        UNSAFE.putInt(i, Int10.pack(offsetX, offsetY, offsetZ));

        UNSAFE.putShort(i + 4, ModelVertexType.encodePosition(posX));
        UNSAFE.putShort(i + 6, ModelVertexType.encodePosition(posY));
        UNSAFE.putShort(i + 8, ModelVertexType.encodePosition(posZ));

        UNSAFE.putInt(i + 12, color);

        UNSAFE.putShort(i + 16, ModelVertexType.encodeBlockTexture(u));
        UNSAFE.putShort(i + 18, ModelVertexType.encodeBlockTexture(v));

        UNSAFE.putInt(i + 20, ModelVertexType.encodeLightMapTexCoord(light));

        this.advance();
    }
}
