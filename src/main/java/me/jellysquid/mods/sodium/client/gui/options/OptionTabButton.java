package me.jellysquid.mods.sodium.client.gui.options;

import net.minecraft.text.Text;

public class OptionTabButton extends OptionTab {
    public final Runnable onclicked;

    public OptionTabButton(Text name, Runnable onclicked) {
        super(name);
        this.onclicked = onclicked;
    }
    
    public void execute() {
        onclicked.run();
    }
}
