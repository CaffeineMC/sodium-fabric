package me.jellysquid.mods.sodium.render.texture;

import net.minecraft.client.texture.Sprite;

public class SpriteUtil {
    public static void markSpriteActive(Sprite sprite) {
        if (sprite instanceof SpriteExtended) {
            ((SpriteExtended) sprite).setActive(true);
        }
    }
}
