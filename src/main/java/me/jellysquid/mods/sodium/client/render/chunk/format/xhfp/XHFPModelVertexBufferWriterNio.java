package me.jellysquid.mods.sodium.client.render.chunk.format.xhfp;

import java.nio.ByteBuffer;

import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferWriterNio;
import me.jellysquid.mods.sodium.client.render.chunk.format.DefaultModelVertexFormats;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexUtil;

public class XHFPModelVertexBufferWriterNio extends VertexBufferWriterNio implements ModelVertexSink {
    public XHFPModelVertexBufferWriterNio(VertexBufferView backingBuffer) {
        super(backingBuffer, DefaultModelVertexFormats.MODEL_VERTEX_XHFP);
    }

    private static final int STRIDE = 48;

    int vertexCount = 0;
    float uSum;
    float vSum;

    @Override
    public void writeQuad(float x, float y, float z, int color, float u, float v, int light) {
        this.writeQuad(x, y, z, color, u, v, light, (short) -1);
    }

    @Override
    public void writeQuad(float x, float y, float z, int color, float u, float v, int light, short blockId) {
        uSum += u;
        vSum += v;

        this.writeQuadInternal(
            ModelVertexUtil.denormalizeFloatAsShort(x),
            ModelVertexUtil.denormalizeFloatAsShort(y),
            ModelVertexUtil.denormalizeFloatAsShort(z),
            color,
            ModelVertexUtil.denormalizeFloatAsShort(u),
            ModelVertexUtil.denormalizeFloatAsShort(v),
            ModelVertexUtil.encodeLightMapTexCoord(light),
            blockId
        );
    }

    private void writeQuadInternal(short x, short y, short z, int color, short u, short v, int light, short blockId) {
        int i = this.writeOffset;

        vertexCount++;
        // NB: uSum and vSum must already be incremented outside of this function.

        ByteBuffer buffer = this.byteBuffer;
        buffer.putShort(i, x);
        buffer.putShort(i + 2, y);
        buffer.putShort(i + 4, z);
        buffer.putInt(i + 8, color);
        buffer.putShort(i + 12, u);
        buffer.putShort(i + 14, v);
        buffer.putInt(i + 16, light);
        // NB: We don't set midTexCoord here, it will be filled in later.
        // tangent
        buffer.put(i + 24, (byte) 255);
        buffer.put(i + 25, (byte) 0);
        buffer.put(i + 26, (byte) 0);
        buffer.put(i + 27, (byte) 255);
        // normal
        buffer.put(i + 28, (byte) 0);
        buffer.put(i + 29, (byte) 255);
        buffer.put(i + 30, (byte) 0);
        // padding
        buffer.put(i + 31, (byte) 0);
        // block ID
        buffer.putFloat(i + 32, blockId);
        buffer.putFloat(i + 36, (short) 0);
        buffer.putFloat(i + 40, (short) 0);
        buffer.putFloat(i + 44, (short) 0);

        if (vertexCount == 4) {
            short midU = ModelVertexUtil.denormalizeFloatAsShort(uSum * 0.25f);
            short midV = ModelVertexUtil.denormalizeFloatAsShort(vSum * 0.25f);
            int midTexCoord = (midV << 16) | midU;

            buffer.putInt(i + 20, midTexCoord);
            buffer.putInt(i + 20 - STRIDE, midTexCoord);
            buffer.putInt(i + 20 - STRIDE * 2, midTexCoord);
            buffer.putInt(i + 20 - STRIDE * 3, midTexCoord);

            vertexCount = 0;
            uSum = 0;
            vSum = 0;
        }


        /*
        .addElement(ChunkMeshAttribute.MID_TEX_COORD, 20, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, true)
            .addElement(ChunkMeshAttribute.TANGENT, 24, GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true)
            .addElement(ChunkMeshAttribute.NORMAL, 28, GlVertexAttributeFormat.UNSIGNED_BYTE, 3, true)
            .addElement(ChunkMeshAttribute.BLOCK_ID, 32, GlVertexAttributeFormat.UNSIGNED_INT, 1, false)
         */

        this.advance();
    }
}
