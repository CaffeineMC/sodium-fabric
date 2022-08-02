package net.caffeinemc.sodium.interop.vanilla.vertex.formats;

import net.caffeinemc.sodium.interop.vanilla.vertex.VanillaVertexFormats;
import net.caffeinemc.sodium.interop.vanilla.vertex.fallback.VertexWriterFallback;
import net.caffeinemc.sodium.render.vertex.VertexSink;
import net.caffeinemc.sodium.render.vertex.buffer.VertexBufferView;
import net.caffeinemc.sodium.render.vertex.buffer.VertexBufferWriterNio;
import net.caffeinemc.sodium.render.vertex.buffer.VertexBufferWriterUnsafe;
import net.caffeinemc.sodium.util.packed.ColorABGR;
import net.caffeinemc.sodium.util.packed.Normal3b;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public interface LineVertexSink extends VertexSink {
    VertexFormat VERTEX_FORMAT = VertexFormats.LINES;

    /**
     * Writes a line vertex to the sink.
     *
     * @param x      The x-position of the vertex
     * @param y      The y-position of the vertex
     * @param z      The z-position of the vertex
     * @param color  The ABGR-packed color of the vertex
     * @param normal The 3 byte packed normal vector of the vertex
     */
    void vertexLine(float x, float y, float z, int color, int normal);

    class WriterFallback extends VertexWriterFallback implements LineVertexSink {
        public WriterFallback(VertexConsumer consumer) {
            super(consumer);
        }

        @Override
        public void vertexLine(float x, float y, float z, int color, int normal) {
            VertexConsumer consumer = this.consumer;
            consumer.vertex(x, y, z);
            consumer.color(ColorABGR.unpackRed(color), ColorABGR.unpackGreen(color), ColorABGR.unpackBlue(color), ColorABGR.unpackAlpha(color));
            consumer.normal(Normal3b.unpackX(normal), Normal3b.unpackY(normal), Normal3b.unpackZ(normal));
            consumer.next();
        }
    }

    class WriterUnsafe extends VertexBufferWriterUnsafe implements LineVertexSink {
        public WriterUnsafe(VertexBufferView backingBuffer) {
            super(backingBuffer, VanillaVertexFormats.LINES);
        }

        @Override
        public void ensureCapacity(int count) {
            super.ensureCapacity(count * 2);
        }

        @Override
        public void vertexLine(float x, float y, float z, int color, int normal) {
            for (int i = 0; i < 2; i++) {
                this.vertexLine0(x, y, z, color, normal);
            }
        }

        private void vertexLine0(float x, float y, float z, int color, int normal) {
            long i = this.writePointer;

            MemoryUtil.memPutFloat(i, x);
            MemoryUtil.memPutFloat(i + 4, y);
            MemoryUtil.memPutFloat(i + 8, z);
            MemoryUtil.memPutInt(i + 12, color);
            MemoryUtil.memPutInt(i + 16, normal);

            this.advance();
        }
    }

    class WriterNio extends VertexBufferWriterNio implements LineVertexSink {
        public WriterNio(VertexBufferView backingBuffer) {
            super(backingBuffer, VanillaVertexFormats.LINES);
        }

        @Override
        public void ensureCapacity(int count) {
            super.ensureCapacity(count * 2);
        }

        @Override
        public void vertexLine(float x, float y, float z, int color, int normal) {
            for (int i = 0; i < 2; i++) {
                this.vertexLine0(x, y, z, color, normal);
            }
        }

        private void vertexLine0(float x, float y, float z, int color, int normal) {
            int i = this.writeOffset;

            ByteBuffer buffer = this.byteBuffer;
            buffer.putFloat(i, x);
            buffer.putFloat(i + 4, y);
            buffer.putFloat(i + 8, z);
            buffer.putInt(i + 12, color);
            buffer.putInt(i + 16, normal);

            this.advance();
        }
    }
}