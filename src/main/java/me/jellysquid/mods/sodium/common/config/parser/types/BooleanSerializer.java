package me.jellysquid.mods.sodium.common.config.parser.types;

import com.moandjiezana.toml.Toml;
import me.jellysquid.mods.sodium.common.config.parser.binding.OptionBinding;

public class BooleanSerializer implements OptionSerializer {
    @Override
    public void read(Toml toml, OptionBinding binding) throws IllegalAccessException {
        Boolean value = toml.getBoolean(binding.getName());

        if (value == null) {
            return;
        }

        binding.setBoolean(value);
    }
}
