package me.jellysquid.mods.sodium.client.gui.options.control;

import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public interface ControlValueFormatter {
    static ControlValueFormatter guiScale() {
        return (v) -> (v == 0) ? Text.method_43469("options.guiScale.auto").getString() : v + "x";
    }

    static ControlValueFormatter fpsLimit() {
        return (v) -> (v == 260) ? Text.method_43469("options.framerateLimit.max").getString() : Text.method_43469("options.framerate", v).getString();
    }

    static ControlValueFormatter brightness() {
        return (v) -> {
            if (v == 0) {
                return Text.method_43469("options.gamma.min").getString();
            } else if (v == 100) {
                return Text.method_43469("options.gamma.max").getString();
            } else {
                return v + "%";
            }
        };
    }

    static ControlValueFormatter biomeBlend() {
        return (v) -> (v == 0) ? Text.method_43469("gui.none").getString() : Text.method_43469("sodium.options.biome_blend.value", v).getString();
    }

    String format(int value);

    static ControlValueFormatter translateVariable(String key) {
        return (v) -> Text.method_43469(key, v).getString();
    }

    static ControlValueFormatter percentage() {
        return (v) -> v + "%";
    }

    static ControlValueFormatter multiplier() {
        return (v) -> v + "x";
    }

    static ControlValueFormatter quantityOrDisabled(String name, String disableText) {
        return (v) -> v == 0 ? disableText : v + " " + name;
    }

    static ControlValueFormatter number() {
        return String::valueOf;
    }
}
