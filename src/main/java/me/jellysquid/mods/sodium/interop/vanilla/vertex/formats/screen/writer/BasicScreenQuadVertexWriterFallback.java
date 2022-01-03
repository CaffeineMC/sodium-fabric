package me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.screen.writer;

import me.jellysquid.mods.sodium.interop.vanilla.vertex.fallback.VertexWriterFallback;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.screen.BasicScreenQuadVertexSink;
import me.jellysquid.mods.sodium.util.packed.ColorABGR;
import net.minecraft.client.render.VertexConsumer;

public class BasicScreenQuadVertexWriterFallback extends VertexWriterFallback implements BasicScreenQuadVertexSink {
    public BasicScreenQuadVertexWriterFallback(VertexConsumer consumer) {
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
