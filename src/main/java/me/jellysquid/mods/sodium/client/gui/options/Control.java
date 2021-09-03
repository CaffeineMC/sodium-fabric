package me.jellysquid.mods.sodium.client.gui.options;

import me.jellysquid.mods.sodium.client.config.render.Option;
import me.jellysquid.mods.sodium.client.gui.values.Dim2i;

public interface Control<T> {
    Option<T> getOption();

    ControlElement<T> createElement(Dim2i dim);

    int getMaxWidth();
}
