package net.caffeinemc.sodium.render.texture;

import net.caffeinemc.sodium.interop.vanilla.mixin.SpriteVisibilityStorage;
import net.minecraft.client.texture.Sprite;

public class SpriteUtil {
    public static void markSpriteActive(Sprite sprite) {
        if (sprite.getContents() instanceof SpriteVisibilityStorage spriteContents) {
            spriteContents.setActive(true);
        }
    }
}
