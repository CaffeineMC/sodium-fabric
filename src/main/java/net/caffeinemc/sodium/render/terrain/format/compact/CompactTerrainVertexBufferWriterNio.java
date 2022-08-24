package net.caffeinemc.sodium.render.terrain.format.compact;

import net.caffeinemc.sodium.render.vertex.buffer.VertexBufferView;
import net.caffeinemc.sodium.render.vertex.buffer.VertexBufferWriterNio;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexFormats;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexSink;

import java.nio.ByteBuffer;
import net.caffeinemc.sodium.util.TextureUtil;

public class CompactTerrainVertexBufferWriterNio extends VertexBufferWriterNio implements TerrainVertexSink {
    public CompactTerrainVertexBufferWriterNio(VertexBufferView backingBuffer) {
        super(backingBuffer, TerrainVertexFormats.COMPACT);
    }

    @Override
    public void writeVertex(float posX, float posY, float posZ, int color, float u, float v, int light) {
        int i = this.writeOffset;

        ByteBuffer buffer = this.byteBuffer;
        buffer.putShort(i + 0, CompactTerrainVertexType.encodePosition(posX));
        buffer.putShort(i + 2, CompactTerrainVertexType.encodePosition(posY));
        buffer.putShort(i + 4, CompactTerrainVertexType.encodePosition(posZ));

        buffer.putInt(i + 8, color);

        buffer.putShort(i + 12, CompactTerrainVertexType.encodeBlockTexture(u));
        buffer.putShort(i + 14, CompactTerrainVertexType.encodeBlockTexture(v));

        buffer.putInt(i + 16, light);

        this.advance();
    }
}
