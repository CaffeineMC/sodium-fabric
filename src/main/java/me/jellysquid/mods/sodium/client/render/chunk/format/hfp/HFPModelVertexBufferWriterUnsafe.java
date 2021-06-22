package me.jellysquid.mods.sodium.client.render.chunk.format.hfp;

import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferWriterUnsafe;
import me.jellysquid.mods.sodium.client.render.chunk.format.DefaultModelVertexFormats;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexUtil;

public class HFPModelVertexBufferWriterUnsafe extends VertexBufferWriterUnsafe implements ModelVertexSink {
    public HFPModelVertexBufferWriterUnsafe(VertexBufferView backingBuffer) {
        super(backingBuffer, DefaultModelVertexFormats.MODEL_VERTEX_HFP);
    }

    @Override
    public void writeVertex(float x, float y, float z, int color, float u, float v, int light, int offset) {
        this.writeQuadInternal(
                ModelVertexUtil.denormalizeVertexPositionFloatAsShort(x),
                ModelVertexUtil.denormalizeVertexPositionFloatAsShort(y),
                ModelVertexUtil.denormalizeVertexPositionFloatAsShort(z),
                color,
                ModelVertexUtil.denormalizeVertexTextureFloatAsShort(u),
                ModelVertexUtil.denormalizeVertexTextureFloatAsShort(v),
                ModelVertexUtil.encodeLightMapTexCoord(light),
                offset
        );
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void writeQuadInternal(short x, short y, short z, int color, short u, short v, int light, int offset) {
        long i = this.writePointer;

        UNSAFE.putShort(i, x);
        UNSAFE.putShort(i + 2, y);
        UNSAFE.putShort(i + 4, z);
        UNSAFE.putInt(i + 8, color);
        UNSAFE.putShort(i + 12, u);
        UNSAFE.putShort(i + 14, v);
        UNSAFE.putInt(i + 16, light);
        UNSAFE.putInt(i + 20, offset);

        this.advance();
    }

}
