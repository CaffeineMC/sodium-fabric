package me.jellysquid.mods.sodium.client.gui.options.binding.compat;

import me.jellysquid.mods.sodium.client.gui.options.binding.OptionBinding;
import net.minecraft.client.options.BooleanOption;
import net.minecraft.client.options.GameOptions;

public class VanillaBooleanOptionBinding implements OptionBinding<GameOptions, Boolean> {
    private final BooleanOption option;

    public VanillaBooleanOptionBinding(BooleanOption option) {
        this.option = option;
    }

    @Override
    public void setValue(GameOptions storage, Boolean value) {
        this.option.set(storage, value.toString());
    }

    @Override
    public Boolean getValue(GameOptions storage) {
        return this.option.get(storage);
    }
}
