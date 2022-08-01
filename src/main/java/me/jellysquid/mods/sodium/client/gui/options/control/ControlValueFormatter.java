package me.jellysquid.mods.sodium.client.gui.options.control;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public interface ControlValueFormatter {
    static ControlValueFormatter guiScale() {
        return (v) -> (v == 0) ? Text.translatable("options.guiScale.auto").getString() : v + "x";
    }

    static ControlValueFormatter resolution() {
        return (v) -> {
            if (MinecraftClient.getInstance().getWindow().getMonitor() == null) {
                return Text.translatable("options.fullscreen.unavailable").getString();
            } else {
                return v == 0 ? Text.translatable("options.fullscreen.current").getString() : Text.literal(MinecraftClient.getInstance().getWindow().getMonitor().getVideoMode(v - 1).toString()).getString();
            }
        };
    }

    static ControlValueFormatter fpsLimit() {
        return (v) -> (v == 260) ? Text.translatable("options.framerateLimit.max").getString() : Text.translatable("options.framerate", v).getString();
    }

    static ControlValueFormatter brightness() {
        return (v) -> {
            if (v == 0) {
                return Text.translatable("options.gamma.min").getString();
            } else if (v == 100) {
                return Text.translatable("options.gamma.max").getString();
            } else {
                return v + "%";
            }
        };
    }

    static ControlValueFormatter biomeBlend() {
        return (v) -> (v == 0) ? Text.translatable("gui.none").getString() : Text.translatable("sodium.options.biome_blend.value", v).getString();
    }

    String format(int value);

    static ControlValueFormatter translateVariable(String key) {
        return (v) -> Text.translatable(key, v).getString();
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
