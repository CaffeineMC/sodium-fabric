package net.caffeinemc.mods.sodium.client.gui.options.storage;

public interface OptionStorage<T> {
    T getData();

    void save();
}
