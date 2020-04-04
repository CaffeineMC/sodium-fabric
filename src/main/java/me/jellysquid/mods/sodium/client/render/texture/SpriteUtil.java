package me.jellysquid.mods.sodium.client.render.texture;

import net.minecraft.client.texture.Sprite;

public class SpriteUtil {
    public static void ensureSpriteReady(Sprite sprite) {
        ((SpriteExtended) sprite).uploadPendingChanges();
    }

    public static void markSpriteActive(Sprite sprite) {
        ((SpriteExtended) sprite).markActive();
    }
}
