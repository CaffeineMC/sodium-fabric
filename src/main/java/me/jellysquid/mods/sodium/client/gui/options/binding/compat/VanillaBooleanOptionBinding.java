package me.jellysquid.mods.sodium.client.gui.options.binding.compat;

import me.jellysquid.mods.sodium.client.gui.options.binding.OptionBinding;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;

public class VanillaBooleanOptionBinding implements OptionBinding<GameOptions, Boolean> {
    private final SimpleOption<Boolean> option;

    public VanillaBooleanOptionBinding(SimpleOption<Boolean> option) {
        this.option = option;
    }

    @Override
    public void setValue(GameOptions storage, Boolean value) {
        this.option.setValue(value);
    }

    @Override
    public Boolean getValue(GameOptions storage) {
        return this.option.getValue();
    }
}
