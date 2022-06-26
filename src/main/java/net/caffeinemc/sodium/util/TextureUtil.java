package net.caffeinemc.sodium.util;

import net.caffeinemc.gfx.api.texture.Texture;
import net.caffeinemc.gfx.opengl.texture.GlTexture;
import net.caffeinemc.sodium.interop.vanilla.mixin.LightmapTextureManagerAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
import org.apache.commons.lang3.Validate;

// TODO: can we cache these?
public class TextureUtil {
    public static Texture getBlockAtlasTexture() {
        MinecraftClient client = MinecraftClient.getInstance();
        TextureManager textureManager = client.getTextureManager();

        AbstractTexture blockAtlasTex = textureManager.getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
        Validate.notNull(blockAtlasTex, "Block atlas texture isn't available?");

        return GlTexture.wrap(blockAtlasTex.getGlId());
    }

    public static Texture getLightTexture() {
        MinecraftClient client = MinecraftClient.getInstance();

        LightmapTextureManagerAccessor lightmapTextureManager =
                ((LightmapTextureManagerAccessor) client.gameRenderer.getLightmapTextureManager());

        AbstractTexture lightTex = lightmapTextureManager.getTexture();
        Validate.notNull(lightTex, "Lightmap texture isn't available?");

        return GlTexture.wrap(lightTex.getGlId());
    }

    public static int encodeLightMapTexCoord(int light) {
        int r = light;

        // Mask off coordinate values outside 0..255
        r &= 0x00FF_00FF;

        // Light coordinates are normalized values, so upcasting requires a shift
        // Scale the coordinates from the range of 0..255 (unsigned byte) into 0..65535 (unsigned short)
        r <<= 8;

        // Add a half-texel offset to each coordinate so we sample from the center of each texel
        r += 0x0800_0800;

        return r;
    }
}
