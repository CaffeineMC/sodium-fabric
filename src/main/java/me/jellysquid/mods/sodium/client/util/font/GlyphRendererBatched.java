package me.jellysquid.mods.sodium.client.util.font;

import me.jellysquid.mods.sodium.client.model.vertex.formats.GlyphVertexSink;
import net.minecraft.client.font.GlyphRenderer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Matrix4f;

public interface GlyphRendererBatched {
    void drawGlyph(boolean italic, float x, float y, Matrix4f matrix, GlyphVertexSink sink, int color, int light);

    void drawRectangle(GlyphRenderer.Rectangle rectangle, Matrix4f matrix, GlyphVertexSink sink, int light);
}
