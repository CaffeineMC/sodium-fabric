package me.jellysquid.mods.sodium.client.gui.options.control;

import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.TextProvider;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;

public class DropdownBoxControl<T extends Enum<T>> implements Control<T> {
    private final Option<T> option;
    private final T[] allowedValues;
    private final String[] names;

    public DropdownBoxControl(Option<T> option, Class<T> enumType) {
        this(option, enumType, enumType.getEnumConstants());
    }

    public DropdownBoxControl(Option<T> option, Class<T> enumType, String[] names) {
        T[] universe = enumType.getEnumConstants();

        Validate.isTrue(universe.length == names.length, "Mismatch between universe length and names array length");
        Validate.notEmpty(universe, "The enum universe must contain at least one item");

        this.option = option;
        this.allowedValues = universe;
        this.names = names;
    }

    public DropdownBoxControl(Option<T> option, Class<T> enumType, T[] allowedValues) {
        T[] universe = enumType.getEnumConstants();

        this.option = option;
        this.allowedValues = allowedValues;
        this.names = new String[universe.length];

        for (int i = 0; i < this.names.length; i++) {
            String name;
            T value = universe[i];

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
    public ControlElement<T> createElement(Dim2i dim) {
        return new DropdownBoxControlElement<>(this.option, dim, this.allowedValues, this.names);
    }

    @Override
    public int getMaxWidth() {
        return 70;
    }

    private static class DropdownBoxControlElement<T extends Enum<T>> extends ControlElement<T> {
        private final T[] allowedValues;
        private final String[] names;
        private boolean isExtended;
        private final List<Dim2i> options = new ArrayList<>();

        public DropdownBoxControlElement(Option<T> option, Dim2i dim, T[] allowedValues, String[] names) {
            super(option, dim);

            this.allowedValues = allowedValues;
            this.names = names;
            this.options.clear();
        }

        @Override
        public void render(MatrixStack matrixStack, int mouseX, int mouseY, float delta) {
            super.render(matrixStack, mouseX, mouseY, delta);

            Enum<T> value = this.option.getValue();
            String name = this.names[value.ordinal()];
            if (this.isExtended) {
                int y = 0;
                boolean renderUp = this.dim.getLimitY() + (this.dim.getHeight() * this.names.length) > MinecraftClient.getInstance().getWindow().getScaledHeight();
                for (String n : this.names) {
                    Dim2i item = new Dim2i(this.dim.getLimitX() - 120, this.dim.getLimitY() + (renderUp ? y - this.dim.getHeight() * 2 : y), 120, this.dim.getHeight());
                    this.options.add(item);
                    String text = item.containsCursor(mouseX, mouseY) ? ">" + n : n;
                    int stringColor = name.equals(n) ? 0xFF55FFFF : 0xFFFFFFFF;
                    int backgroundColor = item.containsCursor(mouseX, mouseY) || name.equals(n) ? 0xE0000000 : 0x90000000;
                    this.drawRect(item.getOriginX(), item.getOriginY(), item.getLimitX(), item.getLimitY(), backgroundColor);
                    this.drawString(matrixStack, text, item.getOriginX() + 4, item.getCenterY() - 4, stringColor);
                    if (renderUp) {
                        y -= item.getHeight();
                    } else {
                        y += item.getHeight();
                    }
                }
                Dim2i area = new Dim2i(this.dim.getLimitX() - 120, this.dim.getLimitY() + (renderUp ? y - this.dim.getHeight() : 0), 120, (renderUp ? -y : y));
                this.hovered = area.containsCursor(mouseX, mouseY);

                if (!this.isMouseOver(mouseX, mouseY)) {
                    this.isExtended = false;
                }
            } else {
                this.hovered = this.dim.containsCursor(mouseX, mouseY);
                this.options.clear();
            }
            int strWidth = this.getStringWidth(name);
            this.drawString(matrixStack, name, this.dim.getLimitX() - strWidth - 6, this.dim.getCenterY() - 4, 0xFFFFFFFF);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.option.isAvailable() && button == 0 && this.dim.containsCursor(mouseX, mouseY)) {
                this.isExtended = !this.isExtended;
                return true;
            }
            for (int i = 0; i < this.options.size(); i++) {
                Dim2i current = this.options.get(i);
                if (current.containsCursor(mouseX, mouseY)) {
                    this.option.setValue(this.options.size() == this.allowedValues.length ? this.allowedValues[i] :
                            this.allowedValues[i - (this.options.size() - this.allowedValues.length)]);
                    this.isExtended = false;
                    this.playClickSound();
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return super.isMouseOver(mouseX, mouseY) || this.isHovered();
        }
    }
}
