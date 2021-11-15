package me.jellysquid.mods.sodium.client.gui.options;

import net.minecraft.text.Text;

public class OptionPageButton extends OptionPage{
    public final Runnable fn;

    public OptionPageButton(Text name, Runnable fn){
        super(name);
        this.fn = fn;
    }
    
    public void execute(){
        fn.run();
    }
}
