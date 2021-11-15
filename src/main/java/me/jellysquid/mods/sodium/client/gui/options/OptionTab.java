package me.jellysquid.mods.sodium.client.gui.options;

import net.minecraft.text.Text;

public class OptionTab {
    private final Text name;

    public OptionTab(Text name) {
        this.name = name;
    }

    public Text getName() {
        return this.name;
    }
}
