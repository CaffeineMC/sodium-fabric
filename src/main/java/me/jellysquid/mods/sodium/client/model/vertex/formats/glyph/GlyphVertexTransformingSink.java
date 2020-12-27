package me.jellysquid.mods.sodium.client.model.vertex.formats.glyph;

import me.jellysquid.mods.sodium.client.model.vertex.transformers.VertexTransformer;
import me.jellysquid.mods.sodium.client.model.vertex.transformers.VertexTransformingSink;

public class GlyphVertexTransformingSink extends VertexTransformingSink<GlyphVertexSink> implements GlyphVertexSink {
    public GlyphVertexTransformingSink(GlyphVertexSink sink, VertexTransformer transformer) {
        super(sink, transformer);
    }

    @Override
    public void writeGlyph(float x, float y, float z, int color, float u, float v, int light) {
        u = this.transformer.transformTextureU(u);
        v = this.transformer.transformTextureV(v);

        this.sink.writeGlyph(x, y, z, color, u, v, light);
    }
}
