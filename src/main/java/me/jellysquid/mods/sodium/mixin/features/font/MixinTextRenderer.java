package me.jellysquid.mods.sodium.mixin.features.font;

import me.jellysquid.mods.sodium.client.util.font.OutlineCharacterVisitor;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.Glyph;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TextRenderer.class)
public abstract class MixinTextRenderer {
    @Shadow
    private static native int tweakTransparency(int argb);

    /**
     * @author JellySquid
     * @reason Avoid FontStorage lookups
     */
    @Overwrite
    public void drawWithOutline(OrderedText text, float x, float y, int color, int outlineColor, Matrix4f matrix, VertexConsumerProvider vertexConsumers, int light) {
        var textRenderer = (TextRenderer) (Object) this;

        int finalOutlineColor = tweakTransparency(outlineColor);
        TextRenderer.Drawer drawer = textRenderer.new Drawer(vertexConsumers, 0.0F, 0.0F, finalOutlineColor, false, matrix, TextRenderer.TextLayerType.NORMAL, light);

        for(int xOffset = -1; xOffset <= 1; ++xOffset) {
            for(int yOffset = -1; yOffset <= 1; ++yOffset) {
                if (xOffset != 0 || yOffset != 0) {
                    text.accept(new OutlineCharacterVisitor(drawer, textRenderer, x, y, xOffset, yOffset, finalOutlineColor));
                }
            }
        }

        TextRenderer.Drawer drawer2 = textRenderer.new Drawer(vertexConsumers, x, y, tweakTransparency(color), false, matrix, TextRenderer.TextLayerType.POLYGON_OFFSET, light);
        text.accept(drawer2);

        drawer2.drawLayer(0, x);
    }
}
