package me.jellysquid.mods.sodium.client.render.texture;

import net.minecraft.client.texture.Sprite;

public class SpriteUtil {
    public static void markSpriteActive(Sprite sprite) {
        ((SpriteContentsExtended) sprite.getContents()).sodium$setActive(true);
    }

    public static boolean hasAnimation(Sprite sprite) {
        return ((SpriteContentsExtended) sprite.getContents()).sodium$hasAnimation();
    }
}
