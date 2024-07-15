package net.caffeinemc.mods.sodium.client.render.texture;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jetbrains.annotations.Nullable;

public class SpriteUtil {
    public static void markSpriteActive(@Nullable TextureAtlasSprite sprite) {
        if (sprite == null) {
            // Can happen in some cases, for example if a mod passes a BakedQuad with a null sprite
            // to a VertexConsumer that does not have a texture element.
            return;
        }

        ((SpriteContentsExtension) sprite.contents()).sodium$setActive(true);
    }

    public static boolean hasAnimation(TextureAtlasSprite sprite) {
        return ((SpriteContentsExtension) sprite.contents()).sodium$hasAnimation();
    }
}
