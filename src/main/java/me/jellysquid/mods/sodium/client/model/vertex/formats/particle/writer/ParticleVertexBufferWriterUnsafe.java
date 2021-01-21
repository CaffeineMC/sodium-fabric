package me.jellysquid.mods.sodium.client.model.vertex.formats.particle.writer;

import me.jellysquid.mods.sodium.client.model.vertex.VanillaVertexTypes;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferWriterUnsafe;
import me.jellysquid.mods.sodium.client.model.vertex.formats.particle.ParticleVertexSink;

public class ParticleVertexBufferWriterUnsafe extends VertexBufferWriterUnsafe implements ParticleVertexSink {
    public ParticleVertexBufferWriterUnsafe(VertexBufferView backingBuffer) {
        super(backingBuffer, VanillaVertexTypes.PARTICLES);
    }

    @Override
    public void writeParticle(float x, float y, float z, float u, float v, int color, int light) {
        long i = this.writePointer;

        UNSAFE.putFloat(i, x);
        UNSAFE.putFloat(i + 4, y);
        UNSAFE.putFloat(i + 8, z);
        UNSAFE.putFloat(i + 12, u);
        UNSAFE.putFloat(i + 16, v);
        UNSAFE.putInt(i + 20, color);
        UNSAFE.putInt(i + 24, light);

        this.advance();
    }
}
