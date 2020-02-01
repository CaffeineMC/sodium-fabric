package me.jellysquid.mods.sodium.client.gui.options.binding.compat;

import me.jellysquid.mods.sodium.client.gui.options.binding.OptionBinding;
import net.minecraft.client.options.DoubleOption;
import net.minecraft.client.options.GameOptions;

public class VanillaDoubleOptionBinding implements OptionBinding<GameOptions, Double> {
    private final DoubleOption option;

    public VanillaDoubleOptionBinding(DoubleOption option) {
        this.option = option;
    }

    @Override
    public void setValue(GameOptions storage, Double value) {
        this.option.set(storage, value);
    }

    @Override
    public Double getValue(GameOptions storage) {
        return this.option.get(storage);
    }
}
