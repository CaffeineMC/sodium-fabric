package net.caffeinemc.sodium.util;

import net.caffeinemc.sodium.interop.vanilla.mixin.LightmapTextureManagerAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
import org.apache.commons.lang3.Validate;

public class TextureUtil {
    public static int getBlockAtlasTexture() {
        MinecraftClient client = MinecraftClient.getInstance();
        TextureManager textureManager = client.getTextureManager();

        AbstractTexture blockAtlasTex = textureManager.getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
        Validate.notNull(blockAtlasTex, "Block atlas texture isn't available?");

        return blockAtlasTex.getGlId();
    }

    public static int getLightTexture() {
        MinecraftClient client = MinecraftClient.getInstance();

        LightmapTextureManagerAccessor lightmapTextureManager =
                ((LightmapTextureManagerAccessor) client.gameRenderer.getLightmapTextureManager());

        AbstractTexture lightTex = lightmapTextureManager.getTexture();
        Validate.notNull(lightTex, "Lightmap texture isn't available?");

        return lightTex.getGlId();
    }
}
