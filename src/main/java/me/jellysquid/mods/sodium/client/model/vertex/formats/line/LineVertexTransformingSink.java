package me.jellysquid.mods.sodium.client.model.vertex.formats.line;

import me.jellysquid.mods.sodium.client.model.vertex.transformers.VertexTransformer;
import me.jellysquid.mods.sodium.client.model.vertex.transformers.VertexTransformingSink;

public class LineVertexTransformingSink extends VertexTransformingSink<LineVertexSink> implements LineVertexSink {
    public LineVertexTransformingSink(LineVertexSink sink, VertexTransformer transformer) {
        super(sink, transformer);
    }

    @Override
    public void vertexLine(float x, float y, float z, int color) {
        this.sink.vertexLine(x, y, z, color);
    }
}
