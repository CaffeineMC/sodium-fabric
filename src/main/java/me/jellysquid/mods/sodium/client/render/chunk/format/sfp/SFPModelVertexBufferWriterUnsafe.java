package me.jellysquid.mods.sodium.client.render.chunk.format.sfp;

import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferWriterUnsafe;
import me.jellysquid.mods.sodium.client.render.chunk.format.DefaultModelVertexFormats;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;

public class SFPModelVertexBufferWriterUnsafe extends VertexBufferWriterUnsafe implements ModelVertexSink {
    public SFPModelVertexBufferWriterUnsafe(VertexBufferView backingBuffer) {
        super(backingBuffer, DefaultModelVertexFormats.MODEL_VERTEX_SFP);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public void writeQuad(float x, float y, float z, int color, float u, float v, int light) {
        long i = this.writePointer;

        UNSAFE.putFloat(i, x);
        UNSAFE.putFloat(i + 4, y);
        UNSAFE.putFloat(i + 8, z);
        UNSAFE.putInt(i + 12, color);
        UNSAFE.putFloat(i + 16, u);
        UNSAFE.putFloat(i + 20, v);
        UNSAFE.putInt(i + 24, encodeLightMapTexCoord(light));

        this.advance();
    }

    /**
     * This moves some work out the shader code and simplifies things a bit. In vanilla, the game encodes light map
     * texture coordinates as two un-normalized unsigned shorts in the range 0..255. Using the fixed-function pipeline,
     * it then applies a matrix transformation which normalizes these coordinates and applies a centering offset. This
     * operation has non-zero overhead and complicates shader code a bit.
     *
     * To work around the problem, this function instead normalizes these light map texture coordinates and applies the
     * centering offset, allowing it to be baked into the vertex data itself.
     *
     * @param light The light map value
     * @return The light map texture coordinates as two unsigned shorts with a center offset applied
     */
    private static int encodeLightMapTexCoord(int light) {
        int sl = (light >> 16) & 255;
        sl = (sl << 8) + 2048;

        int bl = light & 255;
        bl = (bl << 8) + 2048;

        return (sl << 16) | bl;
    }
}
