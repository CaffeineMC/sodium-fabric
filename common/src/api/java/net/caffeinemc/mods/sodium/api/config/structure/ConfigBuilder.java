package net.caffeinemc.mods.sodium.api.config.structure;

import net.minecraft.resources.ResourceLocation;

public interface ConfigBuilder {
    ModOptionsBuilder registerModConfig(String namespace, String name, String version);

    ModOptionsBuilder registerOwnModConfig();

    OptionPageBuilder createOptionPage();

    OptionGroupBuilder createOptionGroup();

    BooleanOptionBuilder createBooleanOption(ResourceLocation id);

    IntegerOptionBuilder createIntegerOption(ResourceLocation id);

    <E extends Enum<E>> EnumOptionBuilder<E> createEnumOption(ResourceLocation id, Class<E> enumClass);
}
