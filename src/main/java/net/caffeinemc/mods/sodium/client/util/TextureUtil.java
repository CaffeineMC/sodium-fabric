package net.caffeinemc.mods.sodium.client.util;

import com.mojang.blaze3d.systems.RenderSystem;

public class TextureUtil {

    /**
     * NOTE: Must be called while a RenderLayer is active.
     */
    public static int getLightTextureId() {
        return RenderSystem.getShaderTexture(2);
    }

    /**
     * NOTE: Must be called while a RenderLayer is active.
     */
    public static int getBlockTextureId() {
        return RenderSystem.getShaderTexture(0);
    }
}
