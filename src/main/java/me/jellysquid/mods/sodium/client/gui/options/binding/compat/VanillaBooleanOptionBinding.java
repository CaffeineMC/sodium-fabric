package me.jellysquid.mods.sodium.client.gui.options.binding.compat;

import me.jellysquid.mods.sodium.client.gui.options.binding.OptionBinding;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;

public class VanillaBooleanOptionBinding implements OptionBinding<Options, Boolean> {
    private final OptionInstance<Boolean> option;

    public VanillaBooleanOptionBinding(OptionInstance<Boolean> option) {
        this.option = option;
    }

    @Override
    public void setValue(Options storage, Boolean value) {
        this.option.set(value);
    }

    @Override
    public Boolean getValue(Options storage) {
        return this.option.get();
    }
}
