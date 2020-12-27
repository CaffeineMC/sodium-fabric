package me.jellysquid.mods.sodium.client.model.vertex;

import net.minecraft.client.render.VertexConsumer;

public interface VertexDrain {
    static VertexDrain of(VertexConsumer consumer) {
        return (VertexDrain) consumer;
    }

    <T extends VertexSink> T createSink(VertexSinkFactory<T> factory);
}
