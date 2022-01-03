package me.jellysquid.mods.sodium.gui.config;

import me.jellysquid.mods.sodium.config.user.options.Option;
import me.jellysquid.mods.sodium.interop.vanilla.math.vector.Dim2i;

public interface Control<T> {
    Option<T> getOption();

    ControlElement<T> createElement(Dim2i dim);

    int getMaxWidth();
}
