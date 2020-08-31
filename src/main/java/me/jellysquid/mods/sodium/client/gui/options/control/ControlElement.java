package me.jellysquid.mods.sodium.client.gui.options.control;

import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.widgets.AbstractWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ControlElement<T> extends AbstractWidget {
    protected final Option<T> option;

    protected final Dim2i dim;

    protected boolean hovered;

    public ControlElement(Option<T> option, Dim2i dim) {
        this.option = option;
        this.dim = dim;
    }

    public boolean isHovered() {
        return this.hovered;
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float delta) {
        Text text = this.option.getName();
        String name = text.getString();
        Text label;

        if (this.hovered && this.font.getWidth(name) > (this.dim.getWidth() - this.option.getControl().getMaxWidth())) {
            name = name.substring(0, Math.min(name.length(), 10)) + "...";
        }

        if (this.option.isAvailable()) {
            if (this.option.hasChanged()) {
                label = new LiteralText(name).append(" *").setStyle(text.getStyle()).formatted(Formatting.ITALIC);
            } else {
                label = new LiteralText(name).setStyle(text.getStyle()).formatted(Formatting.WHITE);
            }
        } else {
            label = new LiteralText(name).setStyle(text.getStyle()).formatted(Formatting.GRAY, Formatting.STRIKETHROUGH);
        }

        this.hovered = this.dim.containsCursor(mouseX, mouseY);


        this.drawRect(this.dim.getOriginX(), this.dim.getOriginY(), this.dim.getLimitX(), this.dim.getLimitY(), this.hovered ? 0xE0000000 : 0x90000000);
        this.drawString(matrixStack, label, this.dim.getOriginX() + 6, this.dim.getCenterY() - 4, 0xFFFFFFFF);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.dim.containsCursor(mouseX, mouseY);
    }

    public Option<T> getOption() {
        return this.option;
    }

    public Dim2i getDimensions() {
        return this.dim;
    }
}
