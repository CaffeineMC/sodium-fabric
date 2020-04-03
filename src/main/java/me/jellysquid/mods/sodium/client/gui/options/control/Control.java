package me.jellysquid.mods.sodium.client.gui.options.control;

import me.jellysquid.mods.sodium.client.gui.options.Option;
import net.minecraft.client.util.Rect2i;

public interface Control<T> {
    Option<T> getOption();

    ControlElement<T> createElement(Rect2i dim);

}
