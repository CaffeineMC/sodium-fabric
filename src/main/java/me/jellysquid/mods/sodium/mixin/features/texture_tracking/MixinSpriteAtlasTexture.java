package me.jellysquid.mods.sodium.mixin.features.texture_tracking;

import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpriteAtlasTexture.class)
public class MixinSpriteAtlasTexture {
    @Inject(method = "getSprite", at = @At("RETURN"))
    private void preReturnSprite(CallbackInfoReturnable<Sprite> cir) {
        Sprite sprite = cir.getReturnValue();

        if (sprite != null) {
            SpriteUtil.markSpriteActive(sprite);
        }
    }
}
