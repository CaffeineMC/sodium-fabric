package me.jellysquid.mods.sodium.client.gui.options.control;

import net.minecraft.network.chat.Component;

public interface ControlValueFormatter {
    static ControlValueFormatter guiScale() {
        return (v) -> (v == 0) ? Component.translatable("options.guiScale.auto") : Component.literal(v + "x");
    }

    static ControlValueFormatter fpsLimit() {
        return (v) -> (v == 260) ? Component.translatable("options.framerateLimit.max") : Component.translatable("options.framerate", v);
    }

    static ControlValueFormatter brightness() {
        return (v) -> {
            if (v == 0) {
                return Component.translatable("options.gamma.min");
            } else if (v == 100) {
                return Component.translatable("options.gamma.max");
            } else {
                return Component.literal(v + "%");
            }
        };
    }

    static ControlValueFormatter biomeBlend() {
        return (v) -> (v == 0) ? Component.translatable("gui.none") : Component.translatable("sodium.options.biome_blend.value", v);
    }

    Component format(int value);

    static ControlValueFormatter translateVariable(String key) {
        return (v) -> Component.translatable(key, v);
    }

    static ControlValueFormatter percentage() {
        return (v) -> Component.literal(v + "%");
    }

    static ControlValueFormatter multiplier() {
        return (v) -> Component.literal(v + "x");
    }

    static ControlValueFormatter quantityOrDisabled(String name, String disableText) {
        return (v) -> Component.literal(v == 0 ? disableText : v + " " + name);
    }

    static ControlValueFormatter number() {
        return (v) -> Component.literal(String.valueOf(v));
    }
}
