package me.jellysquid.mods.sodium.client.gui.widgets;

import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class FlatButtonWidget extends AbstractWidget implements Drawable {
    private final Dim2i dim;
    private final Runnable action;

    private boolean selected;
    private boolean enabled = true;
    private boolean visible = true;

    private Text label;

    public FlatButtonWidget(Dim2i dim, Text label, Runnable action) {
        this.dim = dim;
        this.label = label;
        this.action = action;
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        if (!this.visible) {
            return;
        }

        this.hovered = this.dim.containsCursor(mouseX, mouseY);

        int backgroundColor = this.enabled ? (hovered ? 0xE0000000 : 0x90000000) : 0x60000000;
        int textColor = this.enabled ? 0xFFFFFFFF : 0x90FFFFFF;

        int strWidth = this.font.getWidth(this.label);

        this.drawRect(this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), backgroundColor);
        this.drawString(drawContext, this.label, this.dim.getCenterX() - (strWidth / 2), this.dim.getCenterY() - 4, textColor);

        if (this.enabled && this.selected) {
            this.drawRect(this.dim.x(), this.dim.getLimitY() - 1, this.dim.getLimitX(), this.dim.getLimitY(), 0xFF94E4D3);
        }
        if (this.enabled && this.isFocused()) {
            this.drawBorder(this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY());
        }
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.enabled || !this.visible) {
            return false;
        }

        if (button == 0 && this.dim.containsCursor(mouseX, mouseY)) {
            doAction();

            return true;
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.isFocused())
            return false;

        if (keyCode == InputUtil.GLFW_KEY_ENTER) {
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

    public void setLabel(Text text) {
        this.label = text;
    }

    public Text getLabel() {
        return this.label;
    }

    @Override
    public @Nullable GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
        if (!enabled || !visible)
            return null;
        return super.getNavigationPath(navigation);
    }

    @Override
    public ScreenRect getNavigationFocus() {
        return new ScreenRect(this.dim.x(), this.dim.y(), this.dim.width(), this.dim.height());
    }
}
