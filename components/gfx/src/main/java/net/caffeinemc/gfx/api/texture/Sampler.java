package net.caffeinemc.gfx.api.texture;

/**
 * A sampler object stores the parameters for how a shader should sample from a texture. Unlike most objects, sampler
 * objects are mutable, and their parameters can be changed while they are bound to a texture unit.
 */
public interface Sampler {
    void setParameter(int parameter, int value);

    void setParameter(int parameter, float value);
}
