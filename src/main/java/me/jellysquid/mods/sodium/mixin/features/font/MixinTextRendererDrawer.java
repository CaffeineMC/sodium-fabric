package me.jellysquid.mods.sodium.mixin.features.font;

import me.jellysquid.mods.sodium.client.model.vertex.VanillaVertexTypes;
import me.jellysquid.mods.sodium.client.model.vertex.VertexDrain;
import me.jellysquid.mods.sodium.client.model.vertex.formats.GlyphVertexSink;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import me.jellysquid.mods.sodium.client.util.font.GlyphRendererBatched;
import net.minecraft.client.font.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(targets = "net/minecraft/client/font/TextRenderer$Drawer")
public abstract class MixinTextRendererDrawer {
    @Shadow(aliases = "field_24240", remap = false)
    private TextRenderer parent;

    @Shadow
    @Final
    private Matrix4f matrix;

    @Shadow
    @Final
    VertexConsumerProvider vertexConsumers;

    @Shadow
    @Final
    private TextRenderer.TextLayerType layerType;

    @Shadow
    @Nullable
    private List<GlyphRenderer.Rectangle> rectangles;

    @Shadow
    @Final
    private int light;

    @Shadow
    private float x;

    @Shadow
    private float y;

    @Shadow
    protected abstract void addRectangle(GlyphRenderer.Rectangle rectangle);

    @Shadow
    @Final
    private boolean shadow;

    @Shadow
    @Final
    private float red;

    @Shadow
    @Final
    private float green;

    @Shadow
    @Final
    private float blue;

    @Shadow
    @Final
    private float brightnessMultiplier;

    @Shadow
    @Final
    private float alpha;

    private RenderLayer prevLayer;
    private GlyphVertexSink prevVertexSink;

    private Identifier prevFontStorageId;
    private FontStorage prevFontStorage;

    /**
     * @author JellySquid
     * @reason Better batching
     */
    @Overwrite
    public boolean accept(int i, Style style, int codePoint) {
        FontStorage fontStorage = this.getFontStorage(style.getFont());
        Glyph glyph = fontStorage.getGlyph(codePoint);
        GlyphRenderer glyphRenderer = style.isObfuscated() && codePoint != 32 ? fontStorage.getObfuscatedGlyphRenderer(glyph) : fontStorage.getGlyphRenderer(codePoint);

        boolean bold = style.isBold();
        TextColor textColor = style.getColor();

        int color;

        if (textColor != null) {
            int rgb = textColor.getRgb();

            color = ColorARGB.toABGR(rgb, (int) (this.alpha * 255.0f));
            color = ColorABGR.mul(color, this.brightnessMultiplier, this.brightnessMultiplier, this.brightnessMultiplier);
        } else {
            color = ColorABGR.pack(this.red, this.green, this.blue, this.alpha);
        }

        float left2;
        float left1;

        if (!(glyphRenderer instanceof EmptyGlyphRenderer)) {
            left1 = bold ? glyph.getBoldOffset() : 0.0F;
            left2 = this.shadow ? glyph.getShadowOffset() : 0.0F;

            var layer = glyphRenderer.getLayer(this.layerType);

            if (prevLayer != layer) {
                prevLayer = layer;
                prevVertexSink = VertexDrain.of(this.vertexConsumers.getBuffer(layer))
                        .createSink(VanillaVertexTypes.GLYPHS);
            }

            this.drawGlyph((GlyphRendererBatched) glyphRenderer, bold, style.isItalic(), left1, this.x + left2, this.y + left2, color, this.light, prevVertexSink);
        }

        left1 = glyph.getAdvance(bold);
        left2 = this.shadow ? 1.0F : 0.0F;

        if (style.isStrikethrough()) {
            this.addRectangle(new GlyphRenderer.Rectangle(this.x + left2 - 1.0F, this.y + left2 + 4.5F, this.x + left2 + left1, this.y + left2 + 4.5F - 1.0F, 0.01F, red, green, blue, alpha));
        }

        if (style.isUnderlined()) {
            this.addRectangle(new GlyphRenderer.Rectangle(this.x + left2 - 1.0F, this.y + left2 + 9.0F, this.x + left2 + left1, this.y + left2 + 9.0F - 1.0F, 0.01F, red, green, blue, alpha));
        }

        this.x += left1;

        return true;
    }

    protected FontStorage getFontStorage(Identifier id) {
        if (this.prevFontStorageId == id) {
            return this.prevFontStorage;
        }

        this.prevFontStorageId = id;
        this.prevFontStorage = this.parent.getFontStorage(id);

        return this.prevFontStorage;
    }

    /**
     * @author JellySquid
     * @reason Better batch text rendering
     */
    @Overwrite
    public float drawLayer(int underlineColor, float x) {
        prevLayer = null;
        prevVertexSink = null;
        prevFontStorageId = null;
        prevFontStorage = null;

        if (this.rectangles != null) {
            GlyphRenderer glyphRenderer = this.parent.getFontStorage(Style.DEFAULT_FONT_ID).getRectangleRenderer();
            GlyphRendererBatched glyphRendererBatched = (GlyphRendererBatched) glyphRenderer;

            VertexConsumer vertexConsumer = this.vertexConsumers.getBuffer(glyphRenderer.getLayer(this.layerType));
            GlyphVertexSink sink = VertexDrain.of(vertexConsumer)
                    .createSink(VanillaVertexTypes.GLYPHS);

            sink.ensureCapacity(4 * this.rectangles.size());

            for (GlyphRenderer.Rectangle rectangle : this.rectangles) {
                glyphRendererBatched.drawRectangle(rectangle, this.matrix, sink, this.light);
            }

            sink.flush();
        }

        return this.x;
    }

    private void drawGlyph(GlyphRendererBatched glyphRenderer,
                           boolean bold, boolean italic, float weight,
                           float x, float y,
                           int color,
                           int light, GlyphVertexSink sink) {
        sink.ensureCapacity(4);

        glyphRenderer.drawGlyph(italic, x, y, this.matrix, sink, color, light);

        if (bold) {
            glyphRenderer.drawGlyph(italic, x + weight, y, this.matrix, sink, color, light);
        }

        sink.flush();
    }
}
