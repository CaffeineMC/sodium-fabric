package net.caffeinemc.sodium.interop.vanilla.vertex.formats.generic;

import net.caffeinemc.sodium.interop.vanilla.math.matrix.Matrix4fUtil;
import net.caffeinemc.sodium.interop.vanilla.vertex.VanillaVertexFormats;
import net.caffeinemc.sodium.interop.vanilla.vertex.fallback.VertexWriterFallback;
import net.caffeinemc.sodium.render.vertex.VertexSink;
import net.caffeinemc.sodium.render.vertex.buffer.VertexBufferView;
import net.caffeinemc.sodium.render.vertex.buffer.VertexBufferWriterNio;
import net.caffeinemc.sodium.render.vertex.buffer.VertexBufferWriterUnsafe;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public interface PositionTextureSink extends VertexSink {
    VertexFormat VERTEX_FORMAT = VertexFormats.POSITION_TEXTURE;

    void writeVertex(float x, float y, float z, float u, float v);

    default void writeVertex(Matrix4f matrix, float x, float y, float z, float u, float v) {
        this.writeVertex(Matrix4fUtil.transformVectorX(matrix, x, y, z), Matrix4fUtil.transformVectorY(matrix, x, y, z), Matrix4fUtil.transformVectorZ(matrix, x, y, z), u, v);
    }

    class WriterNio extends VertexBufferWriterNio implements PositionTextureSink {
        public WriterNio(VertexBufferView backingBuffer) {
            super(backingBuffer, VanillaVertexFormats.POSITION_TEXTURE);
        }

        @Override
        public void writeVertex(float x, float y, float z, float u, float v) {
            int i = this.writeOffset;

            ByteBuffer buf = this.byteBuffer;
            buf.putFloat(i, x);
            buf.putFloat(i + 4, y);
            buf.putFloat(i + 8, z);
            buf.putFloat(i + 12, u);
            buf.putFloat(i + 16, v);

            this.advance();
        }
    }

    class WriterUnsafe extends VertexBufferWriterUnsafe implements PositionTextureSink {
        public WriterUnsafe(VertexBufferView backingBuffer) {
            super(backingBuffer, VanillaVertexFormats.POSITION_TEXTURE);
        }

        @Override
        public void writeVertex(float x, float y, float z, float u, float v) {
            long i = this.writePointer;

            MemoryUtil.memPutFloat(i, x);
            MemoryUtil.memPutFloat(i + 4, y);
            MemoryUtil.memPutFloat(i + 8, z);

            MemoryUtil.memPutFloat(i + 12, u);
            MemoryUtil.memPutFloat(i + 16, v);

            this.advance();
        }
    }

    class WriterFallback extends VertexWriterFallback implements PositionTextureSink {
        public WriterFallback(VertexConsumer consumer) {
            super(consumer);
        }

        @Override
        public void writeVertex(float x, float y, float z, float u, float v) {
            this.consumer.vertex(x, y, z);
            this.consumer.texture(u, v);
        }
    }
}