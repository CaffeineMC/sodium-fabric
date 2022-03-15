package net.caffeinemc.sodium.interop.vanilla.vertex.formats.particle.writer;

import net.caffeinemc.sodium.interop.vanilla.vertex.VanillaVertexFormats;
import net.caffeinemc.sodium.render.vertex.buffer.VertexBufferView;
import net.caffeinemc.sodium.render.vertex.buffer.VertexBufferWriterNio;
import net.caffeinemc.sodium.interop.vanilla.vertex.formats.particle.ParticleVertexSink;

import java.nio.ByteBuffer;

public class ParticleVertexBufferWriterNio extends VertexBufferWriterNio implements ParticleVertexSink {
    public ParticleVertexBufferWriterNio(VertexBufferView backingBuffer) {
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
