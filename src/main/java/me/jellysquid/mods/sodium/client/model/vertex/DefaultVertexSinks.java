package me.jellysquid.mods.sodium.client.model.vertex;

import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.formats.glyph.GlyphVertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.formats.glyph.GlyphVertexTransformingSink;
import me.jellysquid.mods.sodium.client.model.vertex.formats.glyph.writer.GlyphVertexBufferWriterNio;
import me.jellysquid.mods.sodium.client.model.vertex.formats.glyph.writer.GlyphVertexBufferWriterUnsafe;
import me.jellysquid.mods.sodium.client.model.vertex.formats.glyph.writer.GlyphVertexWriterFallback;
import me.jellysquid.mods.sodium.client.model.vertex.formats.line.LineVertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.formats.line.LineVertexTransformingSink;
import me.jellysquid.mods.sodium.client.model.vertex.formats.line.writer.LineVertexBufferWriterNio;
import me.jellysquid.mods.sodium.client.model.vertex.formats.line.writer.LineVertexBufferWriterUnsafe;
import me.jellysquid.mods.sodium.client.model.vertex.formats.line.writer.LineVertexWriterFallback;
import me.jellysquid.mods.sodium.client.model.vertex.formats.particle.ParticleVertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.formats.particle.ParticleVertexTransformingSink;
import me.jellysquid.mods.sodium.client.model.vertex.formats.particle.writer.ParticleVertexBufferWriterNio;
import me.jellysquid.mods.sodium.client.model.vertex.formats.particle.writer.ParticleVertexBufferWriterUnsafe;
import me.jellysquid.mods.sodium.client.model.vertex.formats.particle.writer.ParticleVertexWriterFallback;
import me.jellysquid.mods.sodium.client.model.vertex.formats.quad.QuadVertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.formats.quad.QuadVertexTransformingSink;
import me.jellysquid.mods.sodium.client.model.vertex.formats.quad.writer.QuadVertexBufferWriterNio;
import me.jellysquid.mods.sodium.client.model.vertex.formats.quad.writer.QuadVertexBufferWriterUnsafe;
import me.jellysquid.mods.sodium.client.model.vertex.formats.quad.writer.QuadVertexWriterFallback;
import me.jellysquid.mods.sodium.client.model.vertex.transformers.VertexTransformer;
import net.minecraft.client.render.VertexConsumer;

public class DefaultVertexSinks {
    public static final VertexSinkFactory<QuadVertexSink> QUADS = new VertexSinkFactory<QuadVertexSink>() {
        @Override
        public QuadVertexSink createBufferWriter(VertexBufferView buffer, boolean direct) {
            return direct ? new QuadVertexBufferWriterUnsafe(buffer) : new QuadVertexBufferWriterNio(buffer);
        }

        @Override
        public QuadVertexSink createFallbackWriter(VertexConsumer consumer) {
            return new QuadVertexWriterFallback(consumer);
        }

        @Override
        public QuadVertexSink createTransformingSink(QuadVertexSink sink, VertexTransformer transformer) {
            return new QuadVertexTransformingSink(sink, transformer);
        }
    };

    public static final VertexSinkFactory<LineVertexSink> LINES = new VertexSinkFactory<LineVertexSink>() {
        @Override
        public LineVertexSink createBufferWriter(VertexBufferView buffer, boolean direct) {
            return direct ? new LineVertexBufferWriterUnsafe(buffer) : new LineVertexBufferWriterNio(buffer);
        }

        @Override
        public LineVertexSink createFallbackWriter(VertexConsumer consumer) {
            return new LineVertexWriterFallback(consumer);
        }

        @Override
        public LineVertexSink createTransformingSink(LineVertexSink sink, VertexTransformer transformer) {
            return new LineVertexTransformingSink(sink, transformer);
        }
    };

    public static final VertexSinkFactory<GlyphVertexSink> GLYPHS = new VertexSinkFactory<GlyphVertexSink>() {
        @Override
        public GlyphVertexSink createBufferWriter(VertexBufferView buffer, boolean direct) {
            return direct ? new GlyphVertexBufferWriterUnsafe(buffer) : new GlyphVertexBufferWriterNio(buffer);
        }

        @Override
        public GlyphVertexSink createFallbackWriter(VertexConsumer consumer) {
            return new GlyphVertexWriterFallback(consumer);
        }

        @Override
        public GlyphVertexSink createTransformingSink(GlyphVertexSink sink, VertexTransformer transformer) {
            return new GlyphVertexTransformingSink(sink, transformer);
        }
    };

    public static final VertexSinkFactory<ParticleVertexSink> PARTICLES = new VertexSinkFactory<ParticleVertexSink>() {
        @Override
        public ParticleVertexSink createBufferWriter(VertexBufferView buffer, boolean direct) {
            return direct ? new ParticleVertexBufferWriterUnsafe(buffer) : new ParticleVertexBufferWriterNio(buffer);
        }

        @Override
        public ParticleVertexSink createFallbackWriter(VertexConsumer consumer) {
            return new ParticleVertexWriterFallback(consumer);
        }

        @Override
        public ParticleVertexSink createTransformingSink(ParticleVertexSink sink, VertexTransformer transformer) {
            return new ParticleVertexTransformingSink(sink, transformer);
        }
    };
}
