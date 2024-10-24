package net.caffeinemc.mods.sodium.client.gui.widgets;

import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class CenteredFlatWidget extends AbstractWidget implements Renderable {
    private final Dim2i dim;
    private final Runnable action;
    private final boolean isSelectable;

    private @NotNull Style style = Style.defaults();

    private boolean selected;
    private boolean enabled = true;
    private boolean visible = true;

    private Component label;

    public CenteredFlatWidget(Dim2i dim, Component label, Runnable action, boolean isSelectable) {
        this.dim = dim;
        this.label = label;
        this.action = action;
        this.isSelectable = isSelectable;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (!this.visible) {
            return;
        }

        this.hovered = this.isMouseOver(mouseX, mouseY);

        int backgroundColor = this.hovered ? 0x40001101 : (this.selected ? 0x80000000 : 0x40000000);
        int textColor = this.selected || !this.isSelectable ? this.style.textDefault : this.style.textDisabled;

        int strWidth = this.font.width(this.label);

        int x1 = this.getX();
        int y1 = this.getY();
        int x2 = this.getLimitX();
        int y2 = this.getLimitY();

        if (isSelectable) {
            this.drawRect(graphics, x1, y1, x2, y2, backgroundColor);
        }

        if (selected) {
            this.drawRect(graphics, x2 - 3, y1, x2, y2, 0xFFccfdee);
        }

        this.drawString(graphics, this.label, x1 + 8, (int) Math.ceil(((y1 + (this.getHeight() - this.font.lineHeight) * 0.5f))), textColor);

        if (this.enabled && this.isFocused()) {
            this.drawBorder(graphics, x1, y1, x2, y2, -1);
        }
    }

    public int getX() {
        return this.dim.x();
    }

    public int getY() {
        return this.dim.y();
    }

    public int getWidth() {
        return this.dim.width();
    }

    public int getHeight() {
        return this.dim.height();
    }

    public int getLimitX() {
        return this.dim.getLimitX();
    }

    public int getLimitY() {
        return this.dim.getLimitY();
    }

    public void setStyle(@NotNull Style style) {
        Objects.requireNonNull(style);

        this.style = style;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.enabled || !this.visible) {
            return false;
        }

        if (button == 0 && this.isMouseOver(mouseX, mouseY)) {
            doAction();

            return true;
        }

        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= this.getX() && mouseX < this.getLimitX() && mouseY >= this.getY() && mouseY < this.getLimitY();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.isFocused())
            return false;

        if (CommonInputs.selected(keyCode)) {
            doAction();
            return true;
        }

        return false;
    }

    private void doAction() {
        this.action.run();
        this.playClickSound();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setLabel(Component text) {
        this.label = text;
    }

    public Component getLabel() {
        return this.label;
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
        if (!this.enabled || !this.visible)
            return null;
        return super.nextFocusPath(event);
    }

    @Override
    public ScreenRectangle getRectangle() {
        return new ScreenRectangle(this.getX(), this.getY(), this.getWidth(), this.getHeight());
    }

    public static class Style {
        public int bgHovered, bgDefault, bgDisabled;
        public int textDefault, textDisabled;

        public static Style defaults() {
            var style = new Style();
            style.bgHovered = 0xE0000000;
            style.bgDefault = 0x90000000;
            style.bgDisabled = 0x60000000;
            style.textDefault = 0xccfdee;
            style.textDisabled = 0xF06f9090;

            return style;
        }
    }
}
