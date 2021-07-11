package me.jellysquid.mods.sodium.client.model.vertex.formats;

import me.jellysquid.mods.sodium.client.model.vertex.VanillaVertexTypes;
import me.jellysquid.mods.sodium.client.model.vertex.VertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferWriterNio;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferWriterUnsafe;
import me.jellysquid.mods.sodium.client.model.vertex.fallback.VertexWriterFallback;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.client.util.math.Matrix4fExtended;
import me.jellysquid.mods.sodium.client.util.math.MatrixUtil;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
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
        Matrix4fExtended modelMatrix = MatrixUtil.getExtendedMatrix(matrices.getModel());

        float x2 = modelMatrix.transformVecX(x, y, z);
        float y2 = modelMatrix.transformVecY(x, y, z);
        float z2 = modelMatrix.transformVecZ(x, y, z);

        int norm = MatrixUtil.transformNormalVector(dir.getVector(), matrices.getNormal());

        this.writeQuad(x2, y2, z2, color, u, v, light, overlay, norm);
    }

    class WriterUnsafe extends VertexBufferWriterUnsafe implements ModelQuadVertexSink {
        public WriterUnsafe(VertexBufferView backingBuffer) {
            super(backingBuffer, VanillaVertexTypes.QUADS);
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
            super(backingBuffer, VanillaVertexTypes.QUADS);
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
            consumer.normal(Norm3b.unpackX(normal), Norm3b.unpackY(normal), Norm3b.unpackZ(normal));
            consumer.next();
        }
    }
}
