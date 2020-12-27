package me.jellysquid.mods.sodium.client.model.vertex;

import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.transformers.VertexTransformer;
import net.minecraft.client.render.VertexConsumer;

public interface VertexSinkFactory<T extends VertexSink> {
    T createBufferWriter(VertexBufferView buffer, boolean direct);

    T createFallbackWriter(VertexConsumer consumer);

    T createTransformingSink(T sink, VertexTransformer transformer);
}
