package me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.quad.writer;

import me.jellysquid.mods.sodium.interop.vanilla.vertex.fallback.VertexWriterFallback;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.quad.QuadVertexSink;
import me.jellysquid.mods.sodium.util.packed.Normal3b;
import me.jellysquid.mods.sodium.util.packed.ColorABGR;
import net.minecraft.client.render.VertexConsumer;

public class QuadVertexWriterFallback extends VertexWriterFallback implements QuadVertexSink {
    public QuadVertexWriterFallback(VertexConsumer consumer) {
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
