package me.jellysquid.mods.sodium.client.model.vertex.transformers;

import me.jellysquid.mods.sodium.client.model.vertex.VertexSink;

public class VertexTransformingSink<T extends VertexSink> implements VertexSink {
    protected final T sink;
    protected final VertexTransformer transformer;

    public VertexTransformingSink(T sink, VertexTransformer transformer) {
        this.sink = sink;
        this.transformer = transformer;
    }

    @Override
    public void ensureCapacity(int count) {
        this.sink.ensureCapacity(count);
    }

    @Override
    public void flush() {
        this.sink.flush();
    }
}
