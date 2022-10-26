package me.jellysquid.mods.sodium.client.render.texture;

import net.minecraft.client.texture.Sprite;

public class SpriteUtil {
    public static void markSpriteActive(Sprite sprite) {
        if (sprite.getContents() instanceof SpriteExtended) {
            ((SpriteExtended) sprite.getContents()).setActive(true);
        }
    }
}
