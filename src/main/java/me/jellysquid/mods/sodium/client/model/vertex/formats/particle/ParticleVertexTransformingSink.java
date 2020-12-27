package me.jellysquid.mods.sodium.client.model.vertex.formats.particle;

import me.jellysquid.mods.sodium.client.model.vertex.transformers.VertexTransformer;
import me.jellysquid.mods.sodium.client.model.vertex.transformers.VertexTransformingSink;

public class ParticleVertexTransformingSink extends VertexTransformingSink<ParticleVertexSink> implements ParticleVertexSink {
    public ParticleVertexTransformingSink(ParticleVertexSink sink, VertexTransformer transformer) {
        super(sink, transformer);
    }

    @Override
    public void writeParticle(float x, float y, float z, float u, float v, int color, int light) {
        u = this.transformer.transformTextureU(u);
        v = this.transformer.transformTextureV(v);

        this.sink.writeParticle(x, y, z, u, v, color, light);
    }
}
