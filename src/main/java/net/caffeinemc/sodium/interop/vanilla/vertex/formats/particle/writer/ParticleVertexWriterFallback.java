package net.caffeinemc.sodium.interop.vanilla.vertex.formats.particle.writer;

import net.caffeinemc.sodium.interop.vanilla.vertex.fallback.VertexWriterFallback;
import net.caffeinemc.sodium.interop.vanilla.vertex.formats.particle.ParticleVertexSink;
import net.caffeinemc.sodium.util.packed.ColorABGR;
import net.minecraft.client.render.VertexConsumer;

public class ParticleVertexWriterFallback extends VertexWriterFallback implements ParticleVertexSink {
    public ParticleVertexWriterFallback(VertexConsumer consumer) {
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
