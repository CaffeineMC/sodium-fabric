package me.jellysquid.mods.sodium.client.model.vertex.fallback;

import me.jellysquid.mods.sodium.client.model.vertex.VertexSink;
import net.minecraft.client.render.VertexConsumer;

public abstract class VertexWriterFallback implements VertexSink {
    protected final VertexConsumer consumer;

    protected VertexWriterFallback(VertexConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    public void ensureCapacity(int count) {
        // NO-OP
    }

    @Override
    public void flush() {
        // NO-OP
    }
}
