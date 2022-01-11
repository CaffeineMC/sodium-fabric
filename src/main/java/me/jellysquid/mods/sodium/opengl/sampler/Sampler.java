package me.jellysquid.mods.sodium.opengl.sampler;

public interface Sampler {
    void setParameter(int parameter, int value);

    void setParameter(int parameter, float value);

    int handle();
}
