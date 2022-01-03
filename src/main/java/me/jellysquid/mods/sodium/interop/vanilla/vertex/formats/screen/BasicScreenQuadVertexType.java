package me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.screen;

import me.jellysquid.mods.sodium.render.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.screen.writer.BasicScreenQuadVertexBufferWriterNio;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.screen.writer.BasicScreenQuadVertexBufferWriterUnsafe;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.screen.writer.BasicScreenQuadVertexWriterFallback;
import me.jellysquid.mods.sodium.render.vertex.type.BlittableVertexType;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.VanillaVertexType;
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
