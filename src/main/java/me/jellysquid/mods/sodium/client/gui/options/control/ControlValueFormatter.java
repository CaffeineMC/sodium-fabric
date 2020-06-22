package me.jellysquid.mods.sodium.client.gui.options.control;

public interface ControlValueFormatter {
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
