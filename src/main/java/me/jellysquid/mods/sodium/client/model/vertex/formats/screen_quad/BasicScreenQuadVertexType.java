package me.jellysquid.mods.sodium.client.model.vertex.formats.screen_quad;

import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.formats.screen_quad.writer.BasicScreenQuadVertexBufferWriterNio;
import me.jellysquid.mods.sodium.client.model.vertex.formats.screen_quad.writer.BasicScreenQuadVertexBufferWriterUnsafe;
import me.jellysquid.mods.sodium.client.model.vertex.formats.screen_quad.writer.BasicScreenQuadVertexWriterFallback;
import me.jellysquid.mods.sodium.client.model.vertex.type.BlittableVertexType;
import me.jellysquid.mods.sodium.client.model.vertex.type.VanillaVertexType;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;

public class BasicScreenQuadVertexType implements VanillaVertexType<BasicScreenQuadVertexSink>, BlittableVertexType<BasicScreenQuadVertexSink> {
    @Override
    public BasicScreenQuadVertexSink createFallbackWriter(VertexConsumer consumer) {
        return new BasicScreenQuadVertexWriterFallback(consumer);
    }

    @Override
    public BasicScreenQuadVertexSink createBufferWriter(VertexBufferView buffer, boolean direct) {
        return direct ? new BasicScreenQuadVertexBufferWriterUnsafe(buffer) : new BasicScreenQuadVertexBufferWriterNio(buffer);
    }

    @Override
    public VertexFormat getVertexFormat() {
        return BasicScreenQuadVertexSink.VERTEX_FORMAT;
    }

    @Override
    public BlittableVertexType<BasicScreenQuadVertexSink> asBlittable() {
        return this;
    }
}
