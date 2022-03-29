package net.caffeinemc.sodium.render.terrain.format.standard;

import net.caffeinemc.sodium.render.terrain.format.TerrainVertexFormats;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexSink;
import net.caffeinemc.sodium.render.vertex.buffer.VertexBufferView;
import net.caffeinemc.sodium.render.vertex.buffer.VertexBufferWriterUnsafe;
import net.caffeinemc.sodium.util.TextureUtil;
import org.lwjgl.system.MemoryUtil;

public class StandardTerrainVertexBufferWriterUnsafe extends VertexBufferWriterUnsafe implements TerrainVertexSink {
    public StandardTerrainVertexBufferWriterUnsafe(VertexBufferView backingBuffer) {
        super(backingBuffer, TerrainVertexFormats.STANDARD);
    }

    @Override
    public void writeVertex(float posX, float posY, float posZ, int color, float u, float v, int light) {
        long i = this.writePointer;

        MemoryUtil.memPutFloat(i + 0, posX);
        MemoryUtil.memPutFloat(i + 4, posY);
        MemoryUtil.memPutFloat(i + 8, posZ);

        MemoryUtil.memPutInt(i + 12, color);

        MemoryUtil.memPutFloat(i + 16, u);
        MemoryUtil.memPutFloat(i + 20, v);

        MemoryUtil.memPutInt(i + 24, TextureUtil.encodeLightMapTexCoord(light));

        this.advance();
    }
}
