package me.jellysquid.mods.sodium.render.chunk.format;

import me.jellysquid.mods.sodium.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.model.vertex.buffer.VertexBufferWriterUnsafe;
import org.lwjgl.system.MemoryUtil;

public class ModelVertexBufferWriterUnsafe extends VertexBufferWriterUnsafe implements ModelVertexSink {
    public ModelVertexBufferWriterUnsafe(VertexBufferView backingBuffer) {
        super(backingBuffer, ModelVertexType.INSTANCE);
    }

    @Override
    public void writeVertex(float posX, float posY, float posZ, int color, float u, float v, int light, int chunkId, int bits) {
        long i = this.writePointer;

        MemoryUtil.memPutShort(i + 0, ModelVertexType.encodePosition(posX));
        MemoryUtil.memPutShort(i + 2, ModelVertexType.encodePosition(posY));
        MemoryUtil.memPutShort(i + 4, ModelVertexType.encodePosition(posZ));
        MemoryUtil.memPutShort(i + 6, (short) chunkId);

        MemoryUtil.memPutInt(i + 8, color);

        MemoryUtil.memPutShort(i + 12, ModelVertexType.encodeBlockTexture(u));
        MemoryUtil.memPutShort(i + 14, ModelVertexType.encodeBlockTexture(v));

        MemoryUtil.memPutInt(i + 16, ModelVertexType.encodeLightMapTexCoord(light));
        MemoryUtil.memPutInt(i + 20, bits);

        this.advance();
    }
}
