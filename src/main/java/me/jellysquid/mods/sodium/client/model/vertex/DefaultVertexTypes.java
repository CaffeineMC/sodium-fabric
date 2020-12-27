package me.jellysquid.mods.sodium.client.model.vertex;

import me.jellysquid.mods.sodium.client.model.vertex.formats.glyph.GlyphVertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.formats.glyph.GlyphVertexType;
import me.jellysquid.mods.sodium.client.model.vertex.formats.line.LineVertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.formats.line.LineVertexType;
import me.jellysquid.mods.sodium.client.model.vertex.formats.particle.ParticleVertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.formats.particle.ParticleVertexType;
import me.jellysquid.mods.sodium.client.model.vertex.formats.quad.QuadVertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.formats.quad.QuadVertexType;

public class DefaultVertexTypes {
    public static final VertexType<QuadVertexSink> QUADS = new QuadVertexType();
    public static final VertexType<LineVertexSink> LINES = new LineVertexType();
    public static final VertexType<GlyphVertexSink> GLYPHS = new GlyphVertexType();
    public static final VertexType<ParticleVertexSink> PARTICLES = new ParticleVertexType();
}
