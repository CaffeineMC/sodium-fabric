package me.jellysquid.mods.sodium.client.gui.options;

import net.minecraft.util.Formatting;

public enum OptionImpact {
    LOW(Formatting.GREEN, "Low"),
    MEDIUM(Formatting.YELLOW, "Medium"),
    HIGH(Formatting.GOLD, "High"),
    VARIES(Formatting.WHITE, "Varies");

    private final Formatting color;
    private final String text;

    OptionImpact(Formatting color, String text) {
        this.color = color;
        this.text = text;
    }

    public String toDisplayString() {
        return this.color + this.text;
    }
}
