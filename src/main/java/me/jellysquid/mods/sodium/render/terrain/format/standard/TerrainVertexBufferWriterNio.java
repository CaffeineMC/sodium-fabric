package me.jellysquid.mods.sodium.render.terrain.format.standard;

import me.jellysquid.mods.sodium.render.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.render.vertex.buffer.VertexBufferWriterNio;
import me.jellysquid.mods.sodium.render.terrain.format.TerrainVertexFormats;
import me.jellysquid.mods.sodium.render.terrain.format.TerrainVertexSink;

import java.nio.ByteBuffer;

public class TerrainVertexBufferWriterNio extends VertexBufferWriterNio implements TerrainVertexSink {
    public TerrainVertexBufferWriterNio(VertexBufferView backingBuffer) {
        super(backingBuffer, TerrainVertexFormats.STANDARD);
    }

    @Override
    public void writeVertex(float posX, float posY, float posZ, int color, float u, float v, int light) {
        int i = this.writeOffset;

        ByteBuffer buffer = this.byteBuffer;
        buffer.putShort(i + 0, TerrainVertexType.encodePosition(posX));
        buffer.putShort(i + 2, TerrainVertexType.encodePosition(posY));
        buffer.putShort(i + 4, TerrainVertexType.encodePosition(posZ));

        buffer.putInt(i + 8, color);

        buffer.putShort(i + 12, TerrainVertexType.encodeBlockTexture(u));
        buffer.putShort(i + 14, TerrainVertexType.encodeBlockTexture(v));

        buffer.putInt(i + 16, TerrainVertexType.encodeLightMapTexCoord(light));

        this.advance();
    }
}
