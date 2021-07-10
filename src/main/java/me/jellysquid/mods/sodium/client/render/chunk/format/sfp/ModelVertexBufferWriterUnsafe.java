package me.jellysquid.mods.sodium.client.render.chunk.format.sfp;

import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferWriterUnsafe;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkModelVertexFormats;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import org.lwjgl.system.MemoryUtil;

public class ModelVertexBufferWriterUnsafe extends VertexBufferWriterUnsafe implements ModelVertexSink {
    public ModelVertexBufferWriterUnsafe(VertexBufferView backingBuffer) {
        super(backingBuffer, ChunkModelVertexFormats.DEFAULT);
    }

    @Override
    public void writeVertex(int offsetX, int offsetY, int offsetZ, float posX, float posY, float posZ, int color, float u, float v, int light) {
        long i = this.writePointer;

        MemoryUtil.memPutByte(i, (byte) offsetX);
        MemoryUtil.memPutByte(i + 1, (byte) offsetY);
        MemoryUtil.memPutByte(i + 2, (byte) offsetZ);

        MemoryUtil.memPutShort(i + 4, ModelVertexType.encodePosition(posX));
        MemoryUtil.memPutShort(i + 6, ModelVertexType.encodePosition(posY));
        MemoryUtil.memPutShort(i + 8, ModelVertexType.encodePosition(posZ));

        MemoryUtil.memPutInt(i + 12, color);

        MemoryUtil.memPutShort(i + 16, ModelVertexType.encodeBlockTexture(u));
        MemoryUtil.memPutShort(i + 18, ModelVertexType.encodeBlockTexture(v));

        MemoryUtil.memPutInt(i + 20, ModelVertexType.encodeLightMapTexCoord(light));

        this.advance();
    }
}
