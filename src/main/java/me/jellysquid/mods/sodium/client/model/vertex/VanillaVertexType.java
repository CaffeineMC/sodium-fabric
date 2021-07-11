package me.jellysquid.mods.sodium.client.model.vertex;

import me.jellysquid.mods.sodium.client.gl.attribute.BufferVertexFormat;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.type.BlittableVertexType;
import me.jellysquid.mods.sodium.client.model.vertex.type.BufferVertexType;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;

import java.util.function.Function;

public class VanillaVertexType<T extends VertexSink> implements BufferVertexType<T>, BlittableVertexType<T> {
    private final VertexFormat vertexFormat;

    private final Function<VertexConsumer, T> fallbackWriterFactory;
    private final BufferWriterFactory<T> writerFactory;

    public VanillaVertexType(VertexFormat vertexFormat, Function<VertexConsumer, T> fallbackWriterFactory, BufferWriterFactory<T> writerFactory) {
        this.vertexFormat = vertexFormat;
        this.fallbackWriterFactory = fallbackWriterFactory;
        this.writerFactory = writerFactory;
    }

    @Override
    public T createFallbackWriter(VertexConsumer consumer) {
        return this.fallbackWriterFactory.apply(consumer);
    }

    @Override
    public T createBufferWriter(VertexBufferView buffer, boolean direct) {
        return this.writerFactory.apply(buffer, direct);
    }

    @Override
    public BlittableVertexType<T> asBlittable() {
        return this;
    }

    @Override
    public BufferVertexFormat getBufferVertexFormat() {
        return BufferVertexFormat.from(this.vertexFormat);
    }

    public interface BufferWriterFactory<T> {
        T apply(VertexBufferView view, boolean direct);
    }
}
