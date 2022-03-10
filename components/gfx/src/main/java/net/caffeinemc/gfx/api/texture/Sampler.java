package net.caffeinemc.gfx.api.texture;

public interface Sampler {
    void setParameter(int parameter, int value);

    void setParameter(int parameter, float value);

    int handle();
}
