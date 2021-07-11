package me.jellysquid.mods.sodium.client.model.vertex;

import me.jellysquid.mods.sodium.client.model.vertex.formats.GlyphVertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.formats.LineVertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.formats.ModelQuadVertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.formats.ParticleVertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.formats.generic.PositionColorSink;
import me.jellysquid.mods.sodium.client.model.vertex.formats.generic.PositionTextureSink;

public class VanillaVertexTypes {
    public static final VanillaVertexType<ModelQuadVertexSink> QUADS =
            new VanillaVertexType<>(ModelQuadVertexSink.VERTEX_FORMAT,
                    ModelQuadVertexSink.WriterFallback::new,
                    (out, direct) -> direct ? new ModelQuadVertexSink.WriterUnsafe(out) : new ModelQuadVertexSink.WriterNio(out));

    public static final VanillaVertexType<LineVertexSink> LINES =
            new VanillaVertexType<>(LineVertexSink.VERTEX_FORMAT,
                    LineVertexSink.WriterFallback::new,
                    (out, direct) -> direct ? new LineVertexSink.WriterUnsafe(out) : new LineVertexSink.WriterNio(out));

    public static final VanillaVertexType<GlyphVertexSink> GLYPHS =
            new VanillaVertexType<>(GlyphVertexSink.VERTEX_FORMAT,
                    GlyphVertexSink.WriterFallback::new,
                    (out, direct) -> direct ? new GlyphVertexSink.WriterUnsafe(out) : new GlyphVertexSink.WriterNio(out));

    public static final VanillaVertexType<ParticleVertexSink> PARTICLES =
            new VanillaVertexType<>(ParticleVertexSink.VERTEX_FORMAT,
                    ParticleVertexSink.WriterFallback::new,
                    (out, direct) -> direct ? new ParticleVertexSink.WriterUnsafe(out) : new ParticleVertexSink.WriterNio(out));

    public static final VanillaVertexType<PositionColorSink> POSITION_COLOR =
            new VanillaVertexType<>(PositionColorSink.VERTEX_FORMAT,
                    PositionColorSink.WriterFallback::new,
                    (out, direct) -> direct ? new PositionColorSink.WriterUnsafe(out) : new PositionColorSink.WriterNio(out));

    public static final VanillaVertexType<PositionTextureSink> POSITION_TEXTURE =
            new VanillaVertexType<>(PositionTextureSink.VERTEX_FORMAT,
                    PositionTextureSink.WriterFallback::new,
                    (out, direct) -> direct ? new PositionTextureSink.WriterUnsafe(out) : new PositionTextureSink.WriterNio(out));
}
