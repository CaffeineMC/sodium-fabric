package me.jellysquid.mods.sodium.client.gui.options;

import net.minecraft.client.util.TextFormat;

public enum OptionImpact {
    LOW(TextFormat.GREEN, "Low"),
    MEDIUM(TextFormat.YELLOW, "Medium"),
    HIGH(TextFormat.GOLD, "High"),
    EXTREME(TextFormat.RED, "Extreme"),
    VARIES(TextFormat.WHITE, "Varies");

    private final TextFormat color;
    private final String text;

    OptionImpact(TextFormat color, String text) {
        this.color = color;
        this.text = text;
    }

    public String toDisplayString() {
        return this.color + this.text;
    }
}
