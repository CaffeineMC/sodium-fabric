package net.caffeinemc.sodium.interop.vanilla.vertex;

import net.caffeinemc.sodium.interop.vanilla.vertex.formats.glyph.GlyphVertexSink;
import net.caffeinemc.sodium.interop.vanilla.vertex.formats.glyph.GlyphVertexType;
import net.caffeinemc.sodium.interop.vanilla.vertex.formats.line.LineVertexSink;
import net.caffeinemc.sodium.interop.vanilla.vertex.formats.line.LineVertexType;
import net.caffeinemc.sodium.interop.vanilla.vertex.formats.particle.ParticleVertexSink;
import net.caffeinemc.sodium.interop.vanilla.vertex.formats.particle.ParticleVertexType;
import net.caffeinemc.sodium.interop.vanilla.vertex.formats.quad.QuadVertexSink;
import net.caffeinemc.sodium.interop.vanilla.vertex.formats.quad.QuadVertexType;
import net.caffeinemc.sodium.interop.vanilla.vertex.formats.screen.BasicScreenQuadVertexSink;
import net.caffeinemc.sodium.interop.vanilla.vertex.formats.screen.BasicScreenQuadVertexType;

public class VanillaVertexFormats {
    public static final VanillaVertexType<QuadVertexSink> QUADS = new QuadVertexType();
    public static final VanillaVertexType<LineVertexSink> LINES = new LineVertexType();
    public static final VanillaVertexType<GlyphVertexSink> GLYPHS = new GlyphVertexType();
    public static final VanillaVertexType<ParticleVertexSink> PARTICLES = new ParticleVertexType();
    public static final VanillaVertexType<BasicScreenQuadVertexSink> BASIC_SCREEN_QUADS = new BasicScreenQuadVertexType();
}
