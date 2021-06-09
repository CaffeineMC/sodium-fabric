package me.jellysquid.mods.sodium.client.model;

import me.jellysquid.mods.sodium.client.model.vertex.VertexSink;

public class PrimitiveSink<VS extends VertexSink> {
    public final IndexBufferBuilder indices;
    public final VS vertices;

    public PrimitiveSink(IndexBufferBuilder indices, VS vertices) {
        this.indices = indices;
        this.vertices = vertices;
    }
}
