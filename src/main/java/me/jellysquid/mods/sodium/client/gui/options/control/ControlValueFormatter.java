package me.jellysquid.mods.sodium.client.gui.options.control;

import net.minecraft.client.resource.language.I18n;

public interface ControlValueFormatter {
    static ControlValueFormatter guiScale() {
        return (v) -> (v == 0) ? I18n.translate("control_value_formatter.gui_scale_auto") : v + "x";
    }

    static ControlValueFormatter fpsLimit() {
        return (v) -> (v == 260) ? I18n.translate("control_value_formatter.fps_limit_unlimited") : v + " FPS";
    }

    static ControlValueFormatter brightness() {
        return (v) -> {
            if (v == 0) {
                return I18n.translate("control_value_formatter.moody");
            } else if (v == 100) {
                return I18n.translate("control_value_formatter.bright");
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
