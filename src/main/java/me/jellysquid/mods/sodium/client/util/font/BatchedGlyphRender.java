package me.jellysquid.mods.sodium.client.util.font;

import net.minecraft.client.font.GlyphRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Matrix4f;

import java.util.Objects;

public final class BatchedGlyphRender {
    public final GlyphRenderer glyphRenderer;
    public final boolean bold;
    public final boolean italic;
    public final float weight;
    public final float x;
    public final float y;
    public final float red;
    public final float green;
    public final float blue;
    public final float alpha;
    public final int light;

    public BatchedGlyphRender(GlyphRenderer glyphRenderer,
                              boolean bold, boolean italic, float weight,
                              float x, float y,
                              float red, float green, float blue, float alpha,
                              int light) {
        this.glyphRenderer = glyphRenderer;
        this.bold = bold;
        this.italic = italic;
        this.weight = weight;
        this.x = x;
        this.y = y;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
        this.light = light;
    }
}
