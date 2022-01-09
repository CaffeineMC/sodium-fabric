package me.jellysquid.mods.sodium.opengl.sampler;

public interface Sampler {
    void setParameter(int parameter, int value);

    int handle();
}
