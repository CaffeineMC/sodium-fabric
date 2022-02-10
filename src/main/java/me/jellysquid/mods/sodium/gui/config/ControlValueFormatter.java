package me.jellysquid.mods.sodium.gui.config;

import net.minecraft.network.chat.TranslatableComponent;

public interface ControlValueFormatter {
    static ControlValueFormatter guiScale() {
        return (v) -> (v == 0) ? new TranslatableComponent("options.guiScale.auto").getString() : v + "x";
    }

    static ControlValueFormatter fpsLimit() {
        return (v) -> (v == 260) ? new TranslatableComponent("options.framerateLimit.max").getString() : new TranslatableComponent("options.framerate", v).getString();
    }

    static ControlValueFormatter brightness() {
        return (v) -> {
            if (v == 0) {
                return new TranslatableComponent("options.gamma.min").getString();
            } else if (v == 100) {
                return new TranslatableComponent("options.gamma.max").getString();
            } else {
                return v + "%";
            }
        };
    }

    static ControlValueFormatter biomeBlend() {
        return (v) -> (v == 0) ? new TranslatableComponent("gui.none").getString() : new TranslatableComponent("sodium.options.biome_blend.value", v).getString();
    }

    String format(int value);

    static ControlValueFormatter translateVariable(String key) {
        return (v) -> new TranslatableComponent(key, v).getString();
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
