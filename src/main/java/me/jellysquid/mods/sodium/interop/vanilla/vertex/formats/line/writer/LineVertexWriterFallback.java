package me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.line.writer;

import me.jellysquid.mods.sodium.interop.vanilla.vertex.fallback.VertexWriterFallback;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.line.LineVertexSink;
import me.jellysquid.mods.sodium.util.packed.Normal3b;
import me.jellysquid.mods.sodium.util.packed.ColorABGR;
import net.minecraft.client.render.VertexConsumer;

public class LineVertexWriterFallback extends VertexWriterFallback implements LineVertexSink {
    public LineVertexWriterFallback(VertexConsumer consumer) {
        super(consumer);
    }

    @Override
    public void vertexLine(float x, float y, float z, int color, int normal) {
        VertexConsumer consumer = this.consumer;
        consumer.vertex(x, y, z);
        consumer.color(ColorABGR.unpackRed(color), ColorABGR.unpackGreen(color), ColorABGR.unpackBlue(color), ColorABGR.unpackAlpha(color));
        consumer.normal(Normal3b.unpackX(normal), Normal3b.unpackY(normal), Normal3b.unpackZ(normal));
        consumer.next();
    }
}
