package me.jellysquid.mods.sodium.client.gui.widgets;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.util.Rect2i;

public class FlatButtonWidget extends AbstractWidget implements Drawable {
    private final Rect2i dim;
    private final String label;
    private final Runnable action;

    private boolean selected;
    private boolean enabled = true;

    public FlatButtonWidget(Rect2i dim, String label, Runnable action) {
        this.dim = dim;
        this.label = label;
        this.action = action;
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        boolean hovered = this.dim.contains(mouseX, mouseY);

        int x = this.dim.getX();
        int y = this.dim.getY();
        int width = this.dim.getWidth();
        int height = this.dim.getHeight();

        int backgroundColor = this.enabled ? (hovered ? 0xE0000000 : 0x90000000) : 0x60000000;
        int textColor = this.enabled ? 0xFFFFFFFF : 0x90FFFFFF;

        int strWidth = this.font.getStringWidth(this.label);

        this.drawRect(x, y, x + width, y + height, backgroundColor);
        this.drawString(this.label, x + (width / 2) - (strWidth / 2), y + (height / 2) - 4, textColor);

        if (this.enabled && this.selected) {
            this.drawRect(x, y + height - 1, x + width, y + height, 0xFF94E4D3);
        }
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.enabled) {
            return false;
        }

        if (button == 0 && this.dim.contains((int) mouseX, (int) mouseY)) {
            this.action.run();
            this.playClickSound();

            return true;
        }

        return false;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
