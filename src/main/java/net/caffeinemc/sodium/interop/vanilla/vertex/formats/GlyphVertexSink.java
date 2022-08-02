package net.caffeinemc.sodium.interop.vanilla.vertex.formats;
import net.caffeinemc.sodium.interop.vanilla.math.matrix.Matrix4fUtil;
import net.caffeinemc.sodium.interop.vanilla.vertex.VanillaVertexFormats;
import net.caffeinemc.sodium.interop.vanilla.vertex.fallback.VertexWriterFallback;
import net.caffeinemc.sodium.render.vertex.VertexSink;
import net.caffeinemc.sodium.render.vertex.buffer.VertexBufferView;
import net.caffeinemc.sodium.render.vertex.buffer.VertexBufferWriterNio;
import net.caffeinemc.sodium.render.vertex.buffer.VertexBufferWriterUnsafe;
import net.caffeinemc.sodium.util.packed.ColorABGR;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public interface GlyphVertexSink extends VertexSink {
    VertexFormat VERTEX_FORMAT = VertexFormats.POSITION_COLOR_TEXTURE_LIGHT;

    /**
     * Writes a glyph vertex to the sink.
     *
     * @param matrix The transformation matrix to apply to the vertex's position
     * @see GlyphVertexSink#writeGlyph(float, float, float, int, float, float, int)
     */
    default void writeGlyph(Matrix4f matrix, float x, float y, float z, int color, float u, float v, int light) {
        this.writeGlyph(Matrix4fUtil.transformVectorX(matrix, x, y, z), Matrix4fUtil.transformVectorY(matrix, x, y, z), Matrix4fUtil.transformVectorZ(matrix, x, y, z), color, u, v, light);
    }

    /**
     * Writes a glyph vertex to the sink.
     *
     * @param x     The x-position of the vertex
     * @param y     The y-position of the vertex
     * @param z     The z-position of the vertex
     * @param color The ABGR-packed color of the vertex
     * @param u     The u-texture of the vertex
     * @param v     The v-texture of the vertex
     * @param light The packed light map texture coordinates of the vertex
     */
    void writeGlyph(float x, float y, float z, int color, float u, float v, int light);

    class WriterFallback extends VertexWriterFallback implements GlyphVertexSink {
        public WriterFallback(VertexConsumer consumer) {
            super(consumer);
        }

        @Override
        public void writeGlyph(float x, float y, float z, int color, float u, float v, int light) {
            VertexConsumer consumer = this.consumer;
            consumer.vertex(x, y, z);
            consumer.color(ColorABGR.unpackRed(color), ColorABGR.unpackGreen(color), ColorABGR.unpackBlue(color), ColorABGR.unpackAlpha(color));
            consumer.texture(u, v);
            consumer.light(light);
            consumer.next();
        }
    }

    class WriterNio extends VertexBufferWriterNio implements GlyphVertexSink {
        public WriterNio(VertexBufferView backingBuffer) {
            super(backingBuffer, VanillaVertexFormats.GLYPHS);
        }

        @Override
        public void writeGlyph(float x, float y, float z, int color, float u, float v, int light) {
            int i = this.writeOffset;

            ByteBuffer buffer = this.byteBuffer;
            buffer.putFloat(i, x);
            buffer.putFloat(i + 4, y);
            buffer.putFloat(i + 8, z);
            buffer.putInt(i + 12, color);
            buffer.putFloat(i + 16, u);
            buffer.putFloat(i + 20, v);
            buffer.putInt(i + 24, light);

            this.advance();
        }
    }

    class WriterUnsafe extends VertexBufferWriterUnsafe implements GlyphVertexSink {
        public WriterUnsafe(VertexBufferView backingBuffer) {
            super(backingBuffer, VanillaVertexFormats.GLYPHS);
        }

        @Override
        public void writeGlyph(float x, float y, float z, int color, float u, float v, int light) {
            long i = this.writePointer;

            MemoryUtil.memPutFloat(i, x);
            MemoryUtil.memPutFloat(i + 4, y);
            MemoryUtil.memPutFloat(i + 8, z);
            MemoryUtil.memPutInt(i + 12, color);
            MemoryUtil.memPutFloat(i + 16, u);
            MemoryUtil.memPutFloat(i + 20, v);
            MemoryUtil.memPutInt(i + 24, light);

            this.advance();

        }
    }
}