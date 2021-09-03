package me.jellysquid.mods.sodium.client.config.render.binding;

public interface OptionBinding<S, T> {
    void setValue(S storage, T value);

    T getValue(S storage);
}
