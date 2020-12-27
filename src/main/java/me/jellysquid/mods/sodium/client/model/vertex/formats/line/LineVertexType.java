package me.jellysquid.mods.sodium.client.model.vertex.formats.line;

import me.jellysquid.mods.sodium.client.model.vertex.VertexType;
import me.jellysquid.mods.sodium.client.model.vertex.VertexTypeBlittable;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.formats.line.writer.LineVertexBufferWriterNio;
import me.jellysquid.mods.sodium.client.model.vertex.formats.line.writer.LineVertexBufferWriterUnsafe;
import me.jellysquid.mods.sodium.client.model.vertex.formats.line.writer.LineVertexWriterFallback;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;

public class LineVertexType implements VertexType<LineVertexSink>, VertexTypeBlittable<LineVertexSink> {
    @Override
    public LineVertexSink createBufferWriter(VertexBufferView buffer, boolean direct) {
        return direct ? new LineVertexBufferWriterUnsafe(buffer) : new LineVertexBufferWriterNio(buffer);
    }

    @Override
    public LineVertexSink createFallbackWriter(VertexConsumer consumer) {
        return new LineVertexWriterFallback(consumer);
    }

    @Override
    public VertexFormat getBufferVertexFormat() {
        return LineVertexSink.VERTEX_FORMAT;
    }

    @Override
    public VertexTypeBlittable<LineVertexSink> asBlittable() {
        return this;
    }
}
