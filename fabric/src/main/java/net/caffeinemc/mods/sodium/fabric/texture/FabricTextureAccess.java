package net.caffeinemc.mods.sodium.fabric.texture;

import net.caffeinemc.mods.sodium.client.services.PlatformTextureAccess;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class FabricTextureAccess implements PlatformTextureAccess {
    @Override
    public TextureAtlasSprite findInBlockAtlas(float texU, float texV) {
        return SpriteFinderCache.forBlockAtlas().find(texU, texV);
    }
}
