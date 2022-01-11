package me.jellysquid.mods.sodium.render.terrain.format.standard;

import me.jellysquid.mods.sodium.render.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.render.vertex.buffer.VertexBufferWriterUnsafe;
import me.jellysquid.mods.sodium.render.terrain.format.TerrainVertexFormats;
import me.jellysquid.mods.sodium.render.terrain.format.TerrainVertexSink;
import org.lwjgl.system.MemoryUtil;

public class TerrainVertexBufferWriterUnsafe extends VertexBufferWriterUnsafe implements TerrainVertexSink {
    public TerrainVertexBufferWriterUnsafe(VertexBufferView backingBuffer) {
        super(backingBuffer, TerrainVertexFormats.STANDARD);
    }

    @Override
    public void writeVertex(float posX, float posY, float posZ, int color, float u, float v, int light) {
        long i = this.writePointer;

        MemoryUtil.memPutShort(i + 0, TerrainVertexType.encodePosition(posX));
        MemoryUtil.memPutShort(i + 2, TerrainVertexType.encodePosition(posY));
        MemoryUtil.memPutShort(i + 4, TerrainVertexType.encodePosition(posZ));

        MemoryUtil.memPutInt(i + 8, color);

        MemoryUtil.memPutShort(i + 12, TerrainVertexType.encodeBlockTexture(u));
        MemoryUtil.memPutShort(i + 14, TerrainVertexType.encodeBlockTexture(v));

        MemoryUtil.memPutInt(i + 16, TerrainVertexType.encodeLightMapTexCoord(light));

        this.advance();
    }
}
