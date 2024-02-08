package net.caffeinemc.mods.sodium.client.gui.options.control;

import net.caffeinemc.mods.sodium.client.gui.options.Option;
import net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.Nullable;

public class ControlElement<T> extends AbstractWidget {
    protected final Option<T> option;

    protected final Dim2i dim;

    public ControlElement(Option<T> option, Dim2i dim) {
        this.option = option;
        this.dim = dim;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        String name = this.option.getName().getString();
        String label;

        if ((this.hovered || this.isFocused()) && this.font.width(name) > (this.dim.width() - this.option.getControl().getMaxWidth())) {
            name = name.substring(0, Math.min(name.length(), 10)) + "...";
        }

        if (this.option.isAvailable()) {
            if (this.option.hasChanged()) {
                label = ChatFormatting.ITALIC + name + " *";
            } else {
                label = ChatFormatting.WHITE + name;
            }
        } else {
            label = String.valueOf(ChatFormatting.GRAY) + ChatFormatting.STRIKETHROUGH + name;
        }

        this.hovered = this.dim.containsCursor(mouseX, mouseY);

        this.drawRect(graphics, this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), this.hovered ? 0xE0000000 : 0x90000000);
        this.drawString(graphics, label, this.dim.x() + 6, this.dim.getCenterY() - 4, 0xFFFFFFFF);

        if (this.isFocused()) {
            this.drawBorder(graphics, this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), -1);
        }
    }

    public Option<T> getOption() {
        return this.option;
    }

    public Dim2i getDimensions() {
        return this.dim;
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
        if (!this.option.isAvailable())
            return null;
        return super.nextFocusPath(event);
    }

    @Override
    public ScreenRectangle getRectangle() {
        return new ScreenRectangle(this.dim.x(), this.dim.y(), this.dim.width(), this.dim.height());
    }
}
