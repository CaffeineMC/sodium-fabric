package me.jellysquid.mods.sodium.client.gui.options.control;

public interface ControlValueFormatter {
    static ControlValueFormatter guiScale() {
        return (v) -> (v == 0) ? "Auto" : v + "x";
    }

    static ControlValueFormatter fpsLimit() {
        return (v) -> (v == 260) ? "Unlimited" : v + " FPS";
    }

    static ControlValueFormatter brightness() {
        return (v) -> {
            if (v == 0) {
                return "Moody";
            } else if (v == 100) {
                return "Bright";
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
