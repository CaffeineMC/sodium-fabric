package me.jellysquid.mods.sodium.client.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.render.*;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public abstract class AbstractWidget implements Drawable, Element, Selectable {
    protected final TextRenderer font;

    protected AbstractWidget() {
        this.font = MinecraftClient.getInstance().textRenderer;
    }

    protected void drawString(MatrixStack matrixStack, String str, int x, int y, int color) {
        this.font.draw(matrixStack, str, x, y, color);
    }

    protected void drawString(MatrixStack matrixStack, Text text, int x, int y, int color) {
        this.font.draw(matrixStack, text, x, y, color);
    }

    protected void drawRect(double x1, double y1, double x2, double y2, int color) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        this.drawQuads(vertices -> addQuad(vertices, x1, y1, x2, y2, a, r, g, b));
    }

    protected void drawQuads(Consumer<VertexConsumer> consumer) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();

        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        consumer.accept(bufferBuilder);

        BufferBuilder.BuiltBuffer output = bufferBuilder.end();

        BufferRenderer.drawWithShader(output);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    protected static void addQuad(VertexConsumer consumer, double x1, double y1, double x2, double y2, float a, float r, float g, float b) {
        consumer.vertex(x2, y1, 0.0D).color(r, g, b, a).next();
        consumer.vertex(x1, y1, 0.0D).color(r, g, b, a).next();
        consumer.vertex(x1, y2, 0.0D).color(r, g, b, a).next();
        consumer.vertex(x2, y2, 0.0D).color(r, g, b, a).next();
    }

    protected void playClickSound() {
        MinecraftClient.getInstance().getSoundManager()
                .play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    protected int getStringWidth(String text) {
        return this.font.getWidth(text);
    }

    protected int getStringWidth(StringVisitable text) {
        return this.font.getWidth(text);
    }

    public Selectable.SelectionType getType() {
        // FIXME
        return SelectionType.NONE;
    }

    public boolean method_37303() {
        // FIXME
        return true;
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
        // FIXME
    }
}
