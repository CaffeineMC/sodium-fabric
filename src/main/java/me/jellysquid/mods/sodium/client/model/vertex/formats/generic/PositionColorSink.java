package me.jellysquid.mods.sodium.client.model.vertex.formats.generic;

import me.jellysquid.mods.sodium.client.model.vertex.VanillaVertexTypes;
import me.jellysquid.mods.sodium.client.model.vertex.VertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferWriterNio;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferWriterUnsafe;
import me.jellysquid.mods.sodium.client.model.vertex.fallback.VertexWriterFallback;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.client.util.math.Matrix4fExtended;
import me.jellysquid.mods.sodium.client.util.math.MatrixUtil;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public interface PositionColorSink extends VertexSink {
    VertexFormat VERTEX_FORMAT = VertexFormats.POSITION_COLOR;

    /**
     * Writes a quad vertex to this sink.
     *
     * @param x     The x-position of the vertex
     * @param y     The y-position of the vertex
     * @param z     The z-position of the vertex
     * @param color The ABGR-packed color of the vertex
     */
    void writeQuad(float x, float y, float z, int color);

    /**
     * Writes a quad vertex to the sink, transformed by the given matrix.
     *
     * @param matrix The matrix to transform the vertex's position by
     */
    default void writeQuad(Matrix4f matrix, float x, float y, float z, int color) {
        Matrix4fExtended modelMatrix = MatrixUtil.getExtendedMatrix(matrix);

        float x2 = modelMatrix.transformVecX(x, y, z);
        float y2 = modelMatrix.transformVecY(x, y, z);
        float z2 = modelMatrix.transformVecZ(x, y, z);

        this.writeQuad(x2, y2, z2, color);
    }

    class WriterNio extends VertexBufferWriterNio implements PositionColorSink {
        public WriterNio(VertexBufferView backingBuffer) {
            super(backingBuffer, VanillaVertexTypes.POSITION_COLOR);
        }

        @Override
        public void writeQuad(float x, float y, float z, int color) {
            int i = this.writeOffset;

            ByteBuffer buf = this.byteBuffer;
            buf.putFloat(i, x);
            buf.putFloat(i + 4, y);
            buf.putFloat(i + 8, z);
            buf.putInt(i + 12, color);

            this.advance();
        }
    }

    class WriterUnsafe extends VertexBufferWriterUnsafe implements PositionColorSink {
        public WriterUnsafe(VertexBufferView backingBuffer) {
            super(backingBuffer, VanillaVertexTypes.POSITION_COLOR);
        }

        @Override
        public void writeQuad(float x, float y, float z, int color) {
            long i = this.writePointer;

            MemoryUtil.memPutFloat(i, x);
            MemoryUtil.memPutFloat(i + 4, y);
            MemoryUtil.memPutFloat(i + 8, z);
            MemoryUtil.memPutInt(i + 12, color);

            this.advance();
        }
    }

    class WriterFallback extends VertexWriterFallback implements PositionColorSink {
        public WriterFallback(VertexConsumer consumer) {
            super(consumer);
        }

        @Override
        public void writeQuad(float x, float y, float z, int color) {
            VertexConsumer consumer = this.consumer;
            consumer.vertex(x, y, z);
            consumer.color(ColorABGR.unpackRed(color), ColorABGR.unpackGreen(color), ColorABGR.unpackBlue(color), ColorABGR.unpackAlpha(color));
            consumer.next();
        }
    }
}
