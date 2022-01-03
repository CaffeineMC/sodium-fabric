package me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.particle;

import me.jellysquid.mods.sodium.render.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.particle.writer.ParticleVertexBufferWriterNio;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.particle.writer.ParticleVertexBufferWriterUnsafe;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.particle.writer.ParticleVertexWriterFallback;
import me.jellysquid.mods.sodium.render.vertex.type.BlittableVertexType;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.VanillaVertexType;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;

public class ParticleVertexType implements VanillaVertexType<ParticleVertexSink>, BlittableVertexType<ParticleVertexSink> {
    @Override
    public ParticleVertexSink createBufferWriter(VertexBufferView buffer, boolean direct) {
        return direct ? new ParticleVertexBufferWriterUnsafe(buffer) : new ParticleVertexBufferWriterNio(buffer);
    }

    @Override
    public ParticleVertexSink createFallbackWriter(VertexConsumer consumer) {
        return new ParticleVertexWriterFallback(consumer);
    }

    @Override
    public BlittableVertexType<ParticleVertexSink> asBlittable() {
        return this;
    }

    @Override
    public VertexFormat getVertexFormat() {
        return ParticleVertexSink.VERTEX_FORMAT;
    }
}
