package me.jellysquid.mods.sodium.client.gui.options;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.options.control.Control;
import me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Collection;

public interface Option<T> {
    Identifier DEFAULT_ID = new Identifier(SodiumClientMod.ID, "empty");

    default Identifier getId() {
        return DEFAULT_ID;
    }

    Text getName();

    Text getTooltip();

    OptionImpact getImpact();

    Control<T> getControl();

    T getValue();

    void setValue(T value);

    void reset();

    OptionStorage<?> getStorage();

    boolean isAvailable();

    boolean hasChanged();

    void applyChanges();

    Collection<OptionFlag> getFlags();
}
