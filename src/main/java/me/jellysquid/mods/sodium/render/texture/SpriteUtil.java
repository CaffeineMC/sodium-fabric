package me.jellysquid.mods.sodium.render.texture;

import me.jellysquid.mods.sodium.interop.vanilla.mixin.SpriteVisibilityStorage;
import net.minecraft.client.texture.Sprite;

public class SpriteUtil {
    public static void markSpriteActive(Sprite sprite) {
        if (sprite instanceof SpriteVisibilityStorage) {
            ((SpriteVisibilityStorage) sprite).setActive(true);
        }
    }
}
