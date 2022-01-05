package me.jellysquid.mods.sodium.opengl.shader;

public interface Program<T> {
    T getInterface();

    int handle();
}
