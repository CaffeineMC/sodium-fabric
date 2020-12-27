package me.jellysquid.mods.sodium.client.model.vertex.formats.quad;

import me.jellysquid.mods.sodium.client.model.vertex.transformers.VertexTransformer;
import me.jellysquid.mods.sodium.client.model.vertex.transformers.VertexTransformingSink;

public class QuadVertexTransformingSink extends VertexTransformingSink<QuadVertexSink> implements QuadVertexSink {
    public QuadVertexTransformingSink(QuadVertexSink sink, VertexTransformer vertexTransformer) {
        super(sink, vertexTransformer);
    }

    @Override
    public void writeQuad(float x, float y, float z, int color, float u, float v, int light, int overlay, int normal) {
        u = this.transformer.transformTextureU(u);
        v = this.transformer.transformTextureV(v);

        this.sink.writeQuad(x, y, z, color, u, v, light, overlay, normal);
    }
}
