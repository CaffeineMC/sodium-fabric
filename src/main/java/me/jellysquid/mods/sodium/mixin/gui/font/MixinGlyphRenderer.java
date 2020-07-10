package me.jellysquid.mods.sodium.mixin.gui.font;

import me.jellysquid.mods.sodium.client.model.consumer.GlyphVertexConsumer;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import net.minecraft.client.font.GlyphRenderer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GlyphRenderer.class)
public class MixinGlyphRenderer {
    @Shadow
    @Final
    private float xMin;

    @Shadow
    @Final
    private float xMax;

    @Shadow
    @Final
    private float yMin;

    @Shadow
    @Final
    private float yMax;

    @Shadow
    @Final
    private float uMin;

    @Shadow
    @Final
    private float vMin;

    @Shadow
    @Final
    private float vMax;

    @Shadow
    @Final
    private float uMax;

    /**
     * @reason Use intrinsics
     * @author JellySquid
     */
    @Overwrite
    public void draw(boolean italic, float x, float y, Matrix4f matrix, VertexConsumer vertexConsumer, float red, float green, float blue, float alpha, int light) {
        float x1 = x + this.xMin;
        float x2 = x + this.xMax;
        float y1 = this.yMin - 3.0F;
        float y2 = this.yMax - 3.0F;
        float h1 = y + y1;
        float h2 = y + y2;
        float w1 = italic ? 1.0F - 0.25F * y1 : 0.0F;
        float w2 = italic ? 1.0F - 0.25F * y2 : 0.0F;

        int color = ColorABGR.pack(red, green, blue, alpha);

        GlyphVertexConsumer glyphs = ((GlyphVertexConsumer) vertexConsumer);
        glyphs.vertexGlyph(matrix, x1 + w1, h1, 0.0F, color, this.uMin, this.vMin, light);
        glyphs.vertexGlyph(matrix, x1 + w2, h2, 0.0F, color, this.uMin, this.vMax, light);
        glyphs.vertexGlyph(matrix, x2 + w2, h2, 0.0F, color, this.uMax, this.vMax, light);
        glyphs.vertexGlyph(matrix, x2 + w1, h1, 0.0F, color, this.uMax, this.vMin, light);
    }
}
