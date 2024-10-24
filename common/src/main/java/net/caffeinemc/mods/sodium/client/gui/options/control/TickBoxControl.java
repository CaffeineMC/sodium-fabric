package net.caffeinemc.mods.sodium.client.gui.options.control;

import net.caffeinemc.mods.sodium.client.gui.options.Option;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.renderer.Rect2i;

public class TickBoxControl implements Control<Boolean> {
    private final Option<Boolean> option;

    public TickBoxControl(Option<Boolean> option) {
        this.option = option;
    }

    @Override
    public ControlElement<Boolean> createElement(Dim2i dim) {
        return new TickBoxControlElement(this.option, dim);
    }

    @Override
    public int getMaxWidth() {
        return 30;
    }

    @Override
    public Option<Boolean> getOption() {
        return this.option;
    }

    private static class TickBoxControlElement extends ControlElement<Boolean> {
        private final Rect2i button;

        public TickBoxControlElement(Option<Boolean> option, Dim2i dim) {
            super(option, dim);

            this.button = new Rect2i(dim.getLimitX() - 16, dim.getCenterY() - 5, 10, 10);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            super.render(graphics, mouseX, mouseY, delta);

            final int x = this.button.getX();
            final int y = this.button.getY();
            final int w = x + this.button.getWidth();
            final int h = y + this.button.getHeight();

            final boolean enabled = this.option.isAvailable();
            final boolean ticked = enabled && this.option.getValue();

            final int color;

            if (enabled) {
                color = ticked ? 0xFF94E4D3 : 0xFFFFFFFF;
            } else {
                color = 0xFFAAAAAA;
            }

            if (ticked) {
                this.drawRect(graphics, x + 2, y + 2, w - 2, h - 2, color);
            }

            this.drawBorder(graphics, x, y, w, h, color);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.option.isAvailable() && button == 0 && this.dim.containsCursor(mouseX, mouseY)) {
                toggleControl();
                return true;
            }

            return false;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (!isFocused()) return false;

            if (CommonInputs.selected(keyCode)) {
                toggleControl();
                return true;
            }

            return false;
        }

        private void toggleControl() {
            this.playClickSound();
            this.option.setValue(!this.option.getValue());
        }
    }
}
