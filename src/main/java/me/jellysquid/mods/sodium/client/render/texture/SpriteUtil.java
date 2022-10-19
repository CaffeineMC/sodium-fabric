package me.jellysquid.mods.sodium.client.render.texture;

import net.minecraft.client.texture.Sprite;

public class SpriteUtil {
    public static void markSpriteActive(Sprite sprite) {
        if (sprite.method_45851() instanceof SpriteExtended) {
            ((SpriteExtended) sprite.method_45851()).setActive(true);
        }
    }
}
