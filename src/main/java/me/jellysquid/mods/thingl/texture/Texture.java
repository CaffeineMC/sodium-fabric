package me.jellysquid.mods.thingl.texture;

import me.jellysquid.mods.thingl.util.TextureData;

public interface Texture {
    @Deprecated
    void setTextureData(TextureData data);

    @Deprecated
    int getGlId();
}
