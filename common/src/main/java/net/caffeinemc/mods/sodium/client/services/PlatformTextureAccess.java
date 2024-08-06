package net.caffeinemc.mods.sodium.client.services;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public interface PlatformTextureAccess {
    PlatformTextureAccess INSTANCE = Services.load(PlatformTextureAccess.class);

    static PlatformTextureAccess getInstance() {
        return INSTANCE;
    }

    /**
     * Finds the current sprite in the block atlas ({@code TextureAtlas.LOCATION_BLOCKS})
     * @param texU The U coordinate of the texture.
     * @param texV The V coordinate of the texture.
     * @return The sprite in the location in the block atlas.
     */
    TextureAtlasSprite findInBlockAtlas(float texU, float texV);
}
