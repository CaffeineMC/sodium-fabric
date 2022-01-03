package me.jellysquid.mods.sodium.interop.vanilla.vertex;

import me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.glyph.GlyphVertexSink;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.glyph.GlyphVertexType;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.line.LineVertexSink;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.line.LineVertexType;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.particle.ParticleVertexSink;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.particle.ParticleVertexType;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.quad.QuadVertexSink;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.quad.QuadVertexType;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.screen.BasicScreenQuadVertexSink;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.screen.BasicScreenQuadVertexType;

public class VanillaVertexFormats {
    public static final VanillaVertexType<QuadVertexSink> QUADS = new QuadVertexType();
    public static final VanillaVertexType<LineVertexSink> LINES = new LineVertexType();
    public static final VanillaVertexType<GlyphVertexSink> GLYPHS = new GlyphVertexType();
    public static final VanillaVertexType<ParticleVertexSink> PARTICLES = new ParticleVertexType();
    public static final VanillaVertexType<BasicScreenQuadVertexSink> BASIC_SCREEN_QUADS = new BasicScreenQuadVertexType();
}
