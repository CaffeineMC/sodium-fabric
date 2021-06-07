package me.jellysquid.mods.sodium.client.model.vertex.formats.line.writer;

import me.jellysquid.mods.sodium.client.model.vertex.fallback.VertexWriterFallback;
import me.jellysquid.mods.sodium.client.model.vertex.formats.line.LineVertexSink;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import net.minecraft.client.render.VertexConsumer;

public class LineVertexWriterFallback extends VertexWriterFallback implements LineVertexSink {
    public LineVertexWriterFallback(VertexConsumer consumer) {
        super(consumer);
    }

    @Override
    public void vertexLine(
            float x, float y, float z,
            int color,
            float normalX, float normalY, float normalZ) {
        this.consumer
                .vertex(x, y, z)
                .color(ColorABGR.unpackRed(color), ColorABGR.unpackGreen(color), ColorABGR.unpackBlue(color), ColorABGR.unpackAlpha(color))
                .normal(normalX, normalY, normalZ)
                .next();
    }

    @Override
    public void vertexLine(
            float x, float y, float z,
            float r, float g, float b, float a,
            float normalX, float normalY, float normalZ) {
        this.consumer
                .vertex(x, y, z)
                .color(r, g, b, a)
                .normal(normalX, normalY, normalZ)
                .next();
    }
}
