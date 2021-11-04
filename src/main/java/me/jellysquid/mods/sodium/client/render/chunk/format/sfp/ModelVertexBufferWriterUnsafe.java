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
    public void writeVertex(float posX, float posY, float posZ, int color, float u, float v, int light, int chunkId) {
        long i = this.writePointer;

        MemoryUtil.memPutShort(i + 0, ModelVertexType.encodePosition(posX));
        MemoryUtil.memPutShort(i + 2, ModelVertexType.encodePosition(posY));
        MemoryUtil.memPutShort(i + 4, ModelVertexType.encodePosition(posZ));
        MemoryUtil.memPutShort(i + 6, (short) chunkId);

        MemoryUtil.memPutInt(i + 8, color);

        MemoryUtil.memPutShort(i + 12, ModelVertexType.encodeBlockTexture(u));
        MemoryUtil.memPutShort(i + 14, ModelVertexType.encodeBlockTexture(v));

        MemoryUtil.memPutInt(i + 16, ModelVertexType.encodeLightMapTexCoord(light));

        this.advance();
    }
}
