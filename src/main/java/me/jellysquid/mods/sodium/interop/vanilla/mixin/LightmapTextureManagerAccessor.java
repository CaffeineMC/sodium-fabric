package me.jellysquid.mods.sodium.interop.vanilla.mixin;

import net.minecraft.client.renderer.texture.AbstractTexture;

public interface LightmapTextureManagerAccessor {
    AbstractTexture getTexture();
}
