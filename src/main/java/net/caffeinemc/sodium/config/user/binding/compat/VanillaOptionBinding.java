package net.caffeinemc.sodium.config.user.binding.compat;

import net.caffeinemc.sodium.config.user.binding.OptionBinding;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;

public class VanillaOptionBinding<T> implements OptionBinding<GameOptions, T> {
    private final SimpleOption<T> option;

    public VanillaOptionBinding(SimpleOption<T> option) {
        this.option = option;
    }

    @Override
    public void setValue(GameOptions storage, T value) {
        this.option.setValue(value);
    }

    @Override
    public T getValue(GameOptions storage) {
        return this.option.getValue();
    }
}
