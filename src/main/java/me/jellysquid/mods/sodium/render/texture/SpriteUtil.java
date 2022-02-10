package me.jellysquid.mods.sodium.render.texture;

import me.jellysquid.mods.sodium.interop.vanilla.mixin.SpriteVisibilityStorage;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class SpriteUtil {
    public static void markSpriteActive(TextureAtlasSprite sprite) {
        if (sprite instanceof SpriteVisibilityStorage) {
            ((SpriteVisibilityStorage) sprite).setActive(true);
        }
    }
}
