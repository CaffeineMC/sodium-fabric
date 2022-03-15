package net.caffeinemc.sodium.gui.config;

import net.caffeinemc.sodium.config.user.options.Option;
import net.caffeinemc.sodium.interop.vanilla.math.vector.Dim2i;

public interface Control<T> {
    Option<T> getOption();

    ControlElement<T> createElement(Dim2i dim);

    int getMaxWidth();
}
