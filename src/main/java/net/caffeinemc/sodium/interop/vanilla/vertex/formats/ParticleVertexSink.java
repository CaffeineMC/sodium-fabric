package net.caffeinemc.sodium.interop.vanilla.vertex.formats;

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
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public interface ParticleVertexSink extends VertexSink {
    VertexFormat VERTEX_FORMAT = VertexFormats.POSITION_TEXTURE_COLOR_LIGHT;

    /**
     * @param x     The x-position of the vertex
     * @param y     The y-position of the vertex
     * @param z     The z-position of the vertex
     * @param u     The u-texture of the vertex
     * @param v     The v-texture of the vertex
     * @param color The ABGR-packed color of the vertex
     * @param light The packed light map texture coordinates of the vertex
     */
    void writeParticle(float x, float y, float z, float u, float v, int color, int light);

    class WriterFallback extends VertexWriterFallback implements ParticleVertexSink {
        public WriterFallback(VertexConsumer consumer) {
            super(consumer);
        }

        @Override
        public void writeParticle(float x, float y, float z, float u, float v, int color, int light) {
            VertexConsumer consumer = this.consumer;
            consumer.vertex(x, y, z);
            consumer.texture(u, v);
            consumer.color(ColorABGR.unpackRed(color), ColorABGR.unpackGreen(color), ColorABGR.unpackBlue(color), ColorABGR.unpackAlpha(color));
            consumer.light(light);
            consumer.next();
        }
    }

    class WriterNio extends VertexBufferWriterNio implements ParticleVertexSink {
        public WriterNio(VertexBufferView backingBuffer) {
            super(backingBuffer, VanillaVertexFormats.PARTICLES);
        }

        @Override
        public void writeParticle(float x, float y, float z, float u, float v, int color, int light) {
            int i = this.writeOffset;

            ByteBuffer buffer = this.byteBuffer;
            buffer.putFloat(i, x);
            buffer.putFloat(i + 4, y);
            buffer.putFloat(i + 8, z);
            buffer.putFloat(i + 12, u);
            buffer.putFloat(i + 16, v);
            buffer.putInt(i + 20, color);
            buffer.putInt(i + 24, light);

            this.advance();
        }
    }

    class WriterUnsafe extends VertexBufferWriterUnsafe implements ParticleVertexSink {
        public WriterUnsafe(VertexBufferView backingBuffer) {
            super(backingBuffer, VanillaVertexFormats.PARTICLES);
        }

        @Override
        public void writeParticle(float x, float y, float z, float u, float v, int color, int light) {
            long i = this.writePointer;

            MemoryUtil.memPutFloat(i, x);
            MemoryUtil.memPutFloat(i + 4, y);
            MemoryUtil.memPutFloat(i + 8, z);
            MemoryUtil.memPutFloat(i + 12, u);
            MemoryUtil.memPutFloat(i + 16, v);
            MemoryUtil.memPutInt(i + 20, color);
            MemoryUtil.memPutInt(i + 24, light);

            this.advance();
        }
    }
}