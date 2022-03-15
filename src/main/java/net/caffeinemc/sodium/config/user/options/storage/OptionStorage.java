package net.caffeinemc.sodium.config.user.options.storage;

public interface OptionStorage<T> {
    T getData();

    void save();
}
