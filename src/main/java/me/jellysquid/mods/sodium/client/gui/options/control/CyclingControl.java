package me.jellysquid.mods.sodium.client.gui.options.control;

import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.TextProvider;
import net.minecraft.client.util.Rect2i;
import org.apache.commons.lang3.Validate;

public class CyclingControl<T extends Enum<T>> implements Control<T> {
    private final Option<T> option;
    private final T[] universe;
    private final String[] names;

    public CyclingControl(Option<T> option, T[] universe, String[] names) {
        Validate.isTrue(universe.length == names.length, "Mismatch between universe length and names array length");
        Validate.notEmpty(universe, "The enum universe must contain at least one item");

        this.option = option;
        this.universe = universe;
        this.names = names;
    }

    public CyclingControl(Option<T> option, T[] universe) {
        this.option = option;
        this.universe = universe;
        this.names = new String[universe.length];

        for (int i = 0; i < this.names.length; i++) {
            String name;
            T value = this.universe[i];

            if (value instanceof TextProvider) {
                name = ((TextProvider) value).getLocalizedName();
            } else {
                name = value.name();
            }

            this.names[i] = name;
        }
    }

    @Override
    public Option<T> getOption() {
        return this.option;
    }

    @Override
    public ControlElement<T> createElement(Rect2i dim) {
        return new CyclingControlElement<>(this.option, dim, this.universe, this.names);
    }

    private static class CyclingControlElement<T extends Enum<T>> extends ControlElement<T> {
        private final T[] universe;
        private final String[] names;

        public CyclingControlElement(Option<T> option, Rect2i dim, T[] universe, String[] names) {
            super(option, dim);

            this.universe = universe;
            this.names = names;
        }

        @Override
        public void render(int mouseX, int mouseY, float delta) {
            super.render(mouseX, mouseY, delta);

            Enum<T> value = this.option.getValue();
            String name = this.names[value.ordinal()];

            int strWidth = this.getStringWidth(name);
            this.drawString(name, this.dim.getX() + this.dim.getWidth() - strWidth - 6, this.dim.getY() + (this.dim.getHeight() / 2) - 4, 0xFFFFFFFF);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.option.isAvailable() && button == 0 && this.dim.contains((int) mouseX, (int) mouseY)) {
                Enum<T> value = this.option.getValue();
                T next = this.universe[(value.ordinal() + 1) % this.universe.length];

                this.option.setValue(next);
                this.playClickSound();

                return true;
            }

            return false;
        }
    }
}
