package me.jellysquid.mods.thingl.texture;

public interface Sampler {
    @Deprecated
    void bindTextureUnit(int unit);

    void setParameter(int param, int value);

    void setParameter(int param, float value);

    @Deprecated
    void unbindTextureUnit(int unit);
}
