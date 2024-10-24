package net.caffeinemc.mods.sodium.client.config.structure;

import net.caffeinemc.mods.sodium.api.config.*;
import net.caffeinemc.mods.sodium.api.config.option.*;
import net.caffeinemc.mods.sodium.client.config.value.DependentValue;
import net.caffeinemc.mods.sodium.client.gui.options.control.Control;
import net.caffeinemc.mods.sodium.client.gui.options.control.SliderControl;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.EnumSet;
import java.util.function.Function;

class IntegerOption extends Option<Integer> {
    private final DependentValue<Range> range;
    private final ControlValueFormatter valueFormatter;

    IntegerOption(ResourceLocation id, Collection<ResourceLocation> dependencies, Component name, StorageEventHandler storage, Function<Integer, Component> tooltipProvider, OptionImpact impact, EnumSet<OptionFlag> flags, DependentValue<Integer> defaultValue, DependentValue<Boolean> enabled, OptionBinding<Integer> binding, DependentValue<Range> range, ControlValueFormatter valueFormatter) {
        super(id, dependencies, name, storage, tooltipProvider, impact, flags, defaultValue, enabled, binding);
        this.range = range;
        this.valueFormatter = valueFormatter;
    }

    @Override
    boolean isValueValid(Integer value) {
        return this.range.get(this.state).isValueValid(value);
    }

    @Override
    Control<Integer> createControl() {
        var range = this.range.get(this.state);
        return new SliderControl(this, range.min(), range.max(), range.step(), this.valueFormatter);
    }
}

