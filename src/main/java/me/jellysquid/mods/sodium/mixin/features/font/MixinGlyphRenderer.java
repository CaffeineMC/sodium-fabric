package me.jellysquid.mods.sodium.mixin.features.font;

import me.jellysquid.mods.sodium.client.model.vertex.formats.GlyphVertexSink;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.client.util.font.GlyphRendererBatched;
import net.minecraft.client.font.GlyphRenderer;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GlyphRenderer.class)
public class MixinGlyphRenderer implements GlyphRendererBatched {
    @Shadow
    @Final
    private float minX;

    @Shadow
    @Final
    private float maxX;

    @Shadow
    @Final
    private float minY;

    @Shadow
    @Final
    private float maxY;

    @Shadow
    @Final
    private float minU;

    @Shadow
    @Final
    private float minV;

    @Shadow
    @Final
    private float maxV;

    @Shadow
    @Final
    private float maxU;

    @Override
    public void drawGlyph(boolean italic, float x, float y, Matrix4f matrix, GlyphVertexSink sink, int color, int light) {
        float x1 = x + this.minX;
        float x2 = x + this.maxX;
        float y1 = this.minY - 3.0F;
        float y2 = this.maxY - 3.0F;
        float h1 = y + y1;
        float h2 = y + y2;
        float w1 = italic ? 1.0F - 0.25F * y1 : 0.0F;
        float w2 = italic ? 1.0F - 0.25F * y2 : 0.0F;

        sink.writeGlyph(matrix, x1 + w1, h1, 0.0F, color, this.minU, this.minV, light);
        sink.writeGlyph(matrix, x1 + w2, h2, 0.0F, color, this.minU, this.maxV, light);
        sink.writeGlyph(matrix, x2 + w2, h2, 0.0F, color, this.maxU, this.maxV, light);
        sink.writeGlyph(matrix, x2 + w1, h1, 0.0F, color, this.maxU, this.minV, light);
    }

    @Override
    public void drawRectangle(GlyphRenderer.Rectangle rectangle, Matrix4f matrix, GlyphVertexSink sink, int light) {
        int color = ColorABGR.pack(rectangle.red, rectangle.green, rectangle.blue, rectangle.alpha);

        sink.writeGlyph(matrix, rectangle.minX, rectangle.minY, rectangle.zIndex, color, this.minU, this.minV, light);
        sink.writeGlyph(matrix, rectangle.maxX, rectangle.minY, rectangle.zIndex, color, this.minU, this.maxV, light);
        sink.writeGlyph(matrix, rectangle.maxX, rectangle.maxY, rectangle.zIndex, color, this.maxU, this.maxV, light);
        sink.writeGlyph(matrix, rectangle.minX, rectangle.maxY, rectangle.zIndex, color, this.maxU, this.minV, light);
    }
}
