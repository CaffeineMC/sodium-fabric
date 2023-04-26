package me.jellysquid.mods.sodium.client.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.render.*;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public abstract class AbstractWidget implements Drawable, Element, Selectable {
    protected final TextRenderer font;
    protected boolean focused;
    protected boolean hovered;

    protected AbstractWidget() {
        this.font = MinecraftClient.getInstance().textRenderer;
    }

    protected void drawString(DrawContext drawContext, String str, int x, int y, int color) {
        drawContext.drawTextWithShadow(font, str, x, y, color);
    }

    protected void drawString(DrawContext drawContext, Text text, int x, int y, int color) {
        drawContext.drawTextWithShadow(font, text, x, y, color);
    }

    public boolean isHovered() {
        return this.hovered;
    }

    protected void drawRect(double x1, double y1, double x2, double y2, int color) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        this.drawQuads(vertices -> addQuad(vertices, x1, y1, x2, y2, a, r, g, b));
    }

    protected void drawQuads(Consumer<VertexConsumer> consumer) {
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        consumer.accept(bufferBuilder);

        BufferBuilder.BuiltBuffer output = bufferBuilder.end();

        BufferRenderer.drawWithGlobalProgram(output);
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
                .play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
    }

    protected int getStringWidth(String text) {
        return this.font.getWidth(text);
    }

    protected int getStringWidth(StringVisitable text) {
        return this.font.getWidth(text);
    }

    public Selectable.SelectionType getType() {
        if (this.focused) {
            return Selectable.SelectionType.FOCUSED;
        }
        if (this.hovered) {
            return Selectable.SelectionType.HOVERED;
        }
        return Selectable.SelectionType.NONE;
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
        if (focused) {
            builder.put(NarrationPart.USAGE, Text.translatable("narration.button.usage.focused"));
        } else if (hovered) {
            builder.put(NarrationPart.USAGE, Text.translatable("narration.button.usage.hovered"));
        }
    }

    @Nullable
    public GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
        return !this.isFocused() ? GuiNavigationPath.of(this) : null;
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    protected void drawBorder(int x1, int y1, int x2, int y2) {
        this.drawRect(x1, y1, x2, y1 + 1, -1);
        this.drawRect(x1, y2 - 1, x2, y2, -1);
        this.drawRect(x1, y1, x1 + 1, y2, -1);
        this.drawRect(x2 - 1, y1, x2, y2, -1);
    }
}
