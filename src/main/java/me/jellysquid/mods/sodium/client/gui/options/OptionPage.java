package me.jellysquid.mods.sodium.client.gui.options;

import net.minecraft.text.Text;

public class OptionPage {
    private final Text name;

    public OptionPage(Text name) {
        this.name = name;
    }

    public Text getName() {
        return this.name;
    }
}
