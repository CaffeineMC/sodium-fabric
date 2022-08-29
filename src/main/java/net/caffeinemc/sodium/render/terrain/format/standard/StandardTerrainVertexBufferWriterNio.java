package net.caffeinemc.sodium.render.terrain.format.standard;

import net.caffeinemc.sodium.render.terrain.format.TerrainVertexFormats;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexSink;
import net.caffeinemc.sodium.render.vertex.buffer.VertexBufferView;
import net.caffeinemc.sodium.render.vertex.buffer.VertexBufferWriterNio;
import net.caffeinemc.sodium.util.TextureUtil;

import java.nio.ByteBuffer;

public class StandardTerrainVertexBufferWriterNio extends VertexBufferWriterNio implements TerrainVertexSink {
    public StandardTerrainVertexBufferWriterNio(VertexBufferView backingBuffer) {
        super(backingBuffer, TerrainVertexFormats.STANDARD);
    }

    @Override
    public void writeVertex(float posX, float posY, float posZ, int color, float u, float v, int light) {
        int i = this.writeOffset;

        ByteBuffer buffer = this.byteBuffer;
        buffer.putFloat(i + 0, posX);
        buffer.putFloat(i + 4, posY);
        buffer.putFloat(i + 8, posZ);

        buffer.putInt(i + 12, color);

        buffer.putFloat(i + 16, u);
        buffer.putFloat(i + 20, v);

        buffer.putInt(i + 24, light);

        this.advance();
    }
}
