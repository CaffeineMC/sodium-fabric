package net.caffeinemc.sodium.interop.vanilla.vertex.formats;

import net.caffeinemc.sodium.interop.vanilla.math.matrix.Matrix3fUtil;
import net.caffeinemc.sodium.interop.vanilla.math.matrix.Matrix4fUtil;
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
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public interface ModelQuadVertexSink extends VertexSink {
    VertexFormat VERTEX_FORMAT = VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL;

    /**
     * Writes a quad vertex to this sink.
     *
     * @param x       The x-position of the vertex
     * @param y       The y-position of the vertex
     * @param z       The z-position of the vertex
     * @param color   The ABGR-packed color of the vertex
     * @param u       The u-texture of the vertex
     * @param v       The y-texture of the vertex
     * @param light   The packed light-map coordinates of the vertex
     * @param overlay The packed overlay-map coordinates of the vertex
     * @param normal  The 3-byte packed normal vector of the vertex
     */
    void writeQuad(float x, float y, float z, int color, float u, float v, int light, int overlay, int normal);

    /**
     * Writes a quad vertex to the sink, transformed by the given matrices.
     *
     * @param matrices The matrices to transform the vertex's position and normal vectors by
     */
    default void writeQuad(MatrixStack.Entry matrices, float x, float y, float z, int color, float u, float v, int light, int overlay, Direction dir) {
        Matrix4f positionMatrix = matrices.getPositionMatrix();
        this.writeQuad(Matrix4fUtil.transformVectorX(positionMatrix, x, y, z), Matrix4fUtil.transformVectorY(positionMatrix, x, y, z), Matrix4fUtil.transformVectorZ(positionMatrix, x, y, z), color, u, v, light, overlay, Matrix3fUtil.transformNormal(matrices.getNormalMatrix(), dir));
    }

    class WriterUnsafe extends VertexBufferWriterUnsafe implements ModelQuadVertexSink {
        public WriterUnsafe(VertexBufferView backingBuffer) {
            super(backingBuffer, VanillaVertexFormats.QUADS);
        }

        @Override
        public void writeQuad(float x, float y, float z, int color, float u, float v, int light, int overlay, int normal) {
            long i = this.writePointer;

            MemoryUtil.memPutFloat(i, x);
            MemoryUtil.memPutFloat(i + 4, y);
            MemoryUtil.memPutFloat(i + 8, z);
            MemoryUtil.memPutInt(i + 12, color);
            MemoryUtil.memPutFloat(i + 16, u);
            MemoryUtil.memPutFloat(i + 20, v);
            MemoryUtil.memPutInt(i + 24, overlay);
            MemoryUtil.memPutInt(i + 28, light);
            MemoryUtil.memPutInt(i + 32, normal);

            this.advance();
        }
    }

    class WriterNio extends VertexBufferWriterNio implements ModelQuadVertexSink {
        public WriterNio(VertexBufferView backingBuffer) {
            super(backingBuffer, VanillaVertexFormats.QUADS);
        }

        @Override
        public void writeQuad(float x, float y, float z, int color, float u, float v, int light, int overlay, int normal) {
            int i = this.writeOffset;

            ByteBuffer buf = this.byteBuffer;
            buf.putFloat(i, x);
            buf.putFloat(i + 4, y);
            buf.putFloat(i + 8, z);
            buf.putInt(i + 12, color);
            buf.putFloat(i + 16, u);
            buf.putFloat(i + 20, v);
            buf.putInt(i + 24, overlay);
            buf.putInt(i + 28, light);
            buf.putInt(i + 32, normal);

            this.advance();
        }
    }

    class WriterFallback extends VertexWriterFallback implements ModelQuadVertexSink {
        public WriterFallback(VertexConsumer consumer) {
            super(consumer);
        }

        @Override
        public void writeQuad(float x, float y, float z, int color, float u, float v, int light, int overlay, int normal) {
            VertexConsumer consumer = this.consumer;
            consumer.vertex(x, y, z);
            consumer.color(ColorABGR.unpackRed(color), ColorABGR.unpackGreen(color), ColorABGR.unpackBlue(color), ColorABGR.unpackAlpha(color));
            consumer.texture(u, v);
            consumer.overlay(overlay);
            consumer.light(light);
            consumer.normal(Normal3b.unpackX(normal), Normal3b.unpackY(normal), Normal3b.unpackZ(normal));
            consumer.next();
        }
    }
}