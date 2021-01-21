package me.jellysquid.mods.sodium.client.model.vertex.formats.glyph.writer;

import me.jellysquid.mods.sodium.client.model.vertex.fallback.VertexWriterFallback;
import me.jellysquid.mods.sodium.client.model.vertex.formats.glyph.GlyphVertexSink;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import net.minecraft.client.render.VertexConsumer;

public class GlyphVertexWriterFallback extends VertexWriterFallback implements GlyphVertexSink {
    public GlyphVertexWriterFallback(VertexConsumer consumer) {
        super(consumer);
    }

    @Override
    public void writeGlyph(float x, float y, float z, int color, float u, float v, int light) {
        VertexConsumer consumer = this.consumer;
        consumer.vertex(x, y, z);
        consumer.color(ColorABGR.unpackRed(color), ColorABGR.unpackGreen(color), ColorABGR.unpackBlue(color), ColorABGR.unpackAlpha(color));
        consumer.texture(u, v);
        consumer.light(light);
        consumer.next();
    }
}
