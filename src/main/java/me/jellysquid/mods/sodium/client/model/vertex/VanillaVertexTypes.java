package me.jellysquid.mods.sodium.client.model.vertex;

import me.jellysquid.mods.sodium.client.model.vertex.formats.glyph.GlyphVertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.formats.glyph.GlyphVertexType;
import me.jellysquid.mods.sodium.client.model.vertex.formats.line.LineVertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.formats.line.LineVertexType;
import me.jellysquid.mods.sodium.client.model.vertex.formats.particle.ParticleVertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.formats.particle.ParticleVertexType;
import me.jellysquid.mods.sodium.client.model.vertex.formats.quad.QuadVertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.formats.quad.QuadVertexType;
import me.jellysquid.mods.sodium.client.model.vertex.formats.screen_quad.BasicScreenQuadVertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.formats.screen_quad.BasicScreenQuadVertexType;
import me.jellysquid.mods.sodium.client.model.vertex.type.VanillaVertexType;

public class VanillaVertexTypes {
    public static final VanillaVertexType<QuadVertexSink> QUADS = new QuadVertexType();
    public static final VanillaVertexType<LineVertexSink> LINES = new LineVertexType();
    public static final VanillaVertexType<GlyphVertexSink> GLYPHS = new GlyphVertexType();
    public static final VanillaVertexType<ParticleVertexSink> PARTICLES = new ParticleVertexType();
    public static final VanillaVertexType<BasicScreenQuadVertexSink> BASIC_SCREEN_QUADS = new BasicScreenQuadVertexType();
}
