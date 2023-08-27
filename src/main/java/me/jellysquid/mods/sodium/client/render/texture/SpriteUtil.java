package me.jellysquid.mods.sodium.client.render.texture;

import net.minecraft.client.texture.Sprite;
import org.jetbrains.annotations.Nullable;

public class SpriteUtil {
    public static void markSpriteActive(@Nullable Sprite sprite) {
        if (sprite == null) {
            // Can happen in some cases, for example if a mod passes a BakedQuad with a null sprite
            // to a VertexConsumer that does not have a texture element.
            return;
        }

        ((SpriteContentsExtended) sprite.getContents()).sodium$setActive(true);
    }

    public static boolean hasAnimation(Sprite sprite) {
        return ((SpriteContentsExtended) sprite.getContents()).sodium$hasAnimation();
    }
}
