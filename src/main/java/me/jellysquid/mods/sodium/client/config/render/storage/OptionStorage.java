package me.jellysquid.mods.sodium.client.config.render.storage;

public interface OptionStorage<T> {
    T getData();

    void save();
}
