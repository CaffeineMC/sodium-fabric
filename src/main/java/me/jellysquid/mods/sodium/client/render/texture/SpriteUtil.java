package me.jellysquid.mods.sodium.client.render.texture;

import net.minecraft.client.texture.Sprite;

public class SpriteUtil {

    public static void markSpriteActive(Sprite sprite) {
        if (sprite.getContents() instanceof SpriteContentsExtended) {
            ((SpriteContentsExtended) sprite.getContents()).setActive(true);
        }
    }
}
