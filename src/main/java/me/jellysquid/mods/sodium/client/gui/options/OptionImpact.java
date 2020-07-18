package me.jellysquid.mods.sodium.client.gui.options;

import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.Formatting;

public enum OptionImpact {
    LOW(Formatting.GREEN, I18n.translate("sodium.option_impact.low")),
    MEDIUM(Formatting.YELLOW, I18n.translate("sodium.option_impact.medium")),
    HIGH(Formatting.GOLD, I18n.translate("sodium.option_impact.high")),
    EXTREME(Formatting.RED, I18n.translate("sodium.option_impact.extreme")),
    VARIES(Formatting.WHITE, I18n.translate("sodium.option_impact.varies"));

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
