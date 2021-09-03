package me.jellysquid.mods.sodium.config.render.storage;

public interface OptionStorage<T> {
    T getData();

    void save();
}
