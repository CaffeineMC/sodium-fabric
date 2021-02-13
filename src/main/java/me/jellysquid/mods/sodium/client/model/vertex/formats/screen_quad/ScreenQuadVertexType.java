package me.jellysquid.mods.sodium.client.model.vertex.formats.screen_quad;

import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.formats.screen_quad.writer.ScreenQuadVertexBufferWriterNio;
import me.jellysquid.mods.sodium.client.model.vertex.formats.screen_quad.writer.ScreenQuadVertexBufferWriterUnsafe;
import me.jellysquid.mods.sodium.client.model.vertex.formats.screen_quad.writer.ScreenQuadVertexWriterFallback;
import me.jellysquid.mods.sodium.client.model.vertex.type.BlittableVertexType;
import me.jellysquid.mods.sodium.client.model.vertex.type.VanillaVertexType;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;

public class ScreenQuadVertexType implements VanillaVertexType<ScreenQuadVertexSink>, BlittableVertexType<ScreenQuadVertexSink> {
    @Override
    public ScreenQuadVertexSink createFallbackWriter(VertexConsumer consumer) {
        return new ScreenQuadVertexWriterFallback(consumer);
    }

    @Override
    public ScreenQuadVertexSink createBufferWriter(VertexBufferView buffer, boolean direct) {
        return direct ? new ScreenQuadVertexBufferWriterUnsafe(buffer) : new ScreenQuadVertexBufferWriterNio(buffer);
    }

    @Override
    public VertexFormat getVertexFormat() {
        return ScreenQuadVertexSink.VERTEX_FORMAT;
    }

    @Override
    public BlittableVertexType<ScreenQuadVertexSink> asBlittable() {
        return this;
    }
}
