package net.caffeinemc.sodium.interop.vanilla.vertex.formats.particle.writer;

import net.caffeinemc.sodium.interop.vanilla.vertex.VanillaVertexFormats;
import net.caffeinemc.sodium.render.vertex.buffer.VertexBufferView;
import net.caffeinemc.sodium.render.vertex.buffer.VertexBufferWriterUnsafe;
import net.caffeinemc.sodium.interop.vanilla.vertex.formats.particle.ParticleVertexSink;
import org.lwjgl.system.MemoryUtil;

public class ParticleVertexBufferWriterUnsafe extends VertexBufferWriterUnsafe implements ParticleVertexSink {
    public ParticleVertexBufferWriterUnsafe(VertexBufferView backingBuffer) {
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
