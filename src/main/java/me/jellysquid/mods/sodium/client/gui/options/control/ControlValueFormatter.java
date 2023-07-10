package me.jellysquid.mods.sodium.client.gui.options.control;

import net.minecraft.text.Text;

public interface ControlValueFormatter {
    static ControlValueFormatter guiScale() {
        return (v) -> (v == 0) ? Text.translatable("options.guiScale.auto") : Text.literal(v + "x");
    }

    static ControlValueFormatter fpsLimit() {
        return (v) -> (v == 260) ? Text.translatable("options.framerateLimit.max") : Text.translatable("options.framerate", v);
    }

    static ControlValueFormatter brightness() {
        return (v) -> {
            if (v == 0) {
                return Text.translatable("options.gamma.min");
            } else if (v == 100) {
                return Text.translatable("options.gamma.max");
            } else {
                return Text.literal(v + "%");
            }
        };
    }

    static ControlValueFormatter biomeBlend() {
        return (v) -> (v == 0) ? Text.translatable("gui.none") : Text.translatable("sodium.options.biome_blend.value", v);
    }

    Text format(int value);

    static ControlValueFormatter translateVariable(String key) {
        return (v) -> Text.translatable(key, v);
    }

    static ControlValueFormatter percentage() {
        return (v) -> Text.literal(v + "%");
    }

    static ControlValueFormatter multiplier() {
        return (v) -> Text.literal(v + "%");
    }

    static ControlValueFormatter quantityOrDisabled(String name, String disableText) {
        return (v) -> Text.literal(v == 0 ? disableText : v + " " + name);
    }

    static ControlValueFormatter number() {
        return (v) -> Text.literal(String.valueOf(v));
    }
}
