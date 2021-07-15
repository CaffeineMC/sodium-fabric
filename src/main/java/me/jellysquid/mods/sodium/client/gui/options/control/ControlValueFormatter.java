package me.jellysquid.mods.sodium.client.gui.options.control;

import net.minecraft.text.TranslatableText;

public interface ControlValueFormatter {
    static ControlValueFormatter guiScale() {
        return (v) -> (v == 0) ? new TranslatableText("options.guiScale.auto").getString() : v + "x";
    }

    static ControlValueFormatter fpsLimit() {
        return (v) -> (v == 260) ? new TranslatableText("options.framerateLimit.max").getString() : v + " FPS";
    }

    static ControlValueFormatter brightness() {
        return (v) -> {
            if (v == 0) {
                return new TranslatableText("options.gamma.min").getString();
            } else if (v == 100) {
                return new TranslatableText("options.gamma.max").getString();
            } else {
                return v + "%";
            }
        };
    }

    String format(int value);

    static ControlValueFormatter percentage() {
        return (v) -> v + "%";
    }

    static ControlValueFormatter multiplier() {
        return (v) -> v + "x";
    }

    static ControlValueFormatter quantity(String name) {
        return (v) -> v + " " + name;
    }

    static ControlValueFormatter quantityOrDisabled(String name, String disableText) {
        return (v) -> v == 0 ? disableText : v + " " + name;
    }

    static ControlValueFormatter number() {
        return String::valueOf;
    }
}
