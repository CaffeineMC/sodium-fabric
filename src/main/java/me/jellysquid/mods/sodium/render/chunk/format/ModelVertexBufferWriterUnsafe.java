package me.jellysquid.mods.sodium.render.chunk.format;

import me.jellysquid.mods.sodium.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.model.vertex.buffer.VertexBufferWriterUnsafe;
import org.lwjgl.system.MemoryUtil;

public class ModelVertexBufferWriterUnsafe extends VertexBufferWriterUnsafe implements ModelVertexSink {
    public ModelVertexBufferWriterUnsafe(VertexBufferView backingBuffer) {
        super(backingBuffer, ModelVertexType.INSTANCE);
    }

    @Override
    public void writeVertex(long position, int color, int blockTexture, int lightTexture, short chunkIndex, int materialBits) {
        long i = this.writePointer;

        MemoryUtil.memPutLong(i, position);
        MemoryUtil.memPutShort(i + 6, chunkIndex);
        MemoryUtil.memPutInt(i + 8, color);
        MemoryUtil.memPutInt(i + 12, blockTexture);
        MemoryUtil.memPutInt(i + 16, lightTexture);
        MemoryUtil.memPutInt(i + 20, materialBits);

        this.advance();
    }
}
