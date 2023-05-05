package me.jellysquid.mods.sodium.mixin.features.texture_tracking;

import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrawContext.class)
public class MixinDrawableHelper {
    @Inject(method = {
            "drawSprite(IIIIILnet/minecraft/client/texture/Sprite;)V",
            "drawSprite(IIIIILnet/minecraft/client/texture/Sprite;FFFF)V"
    }, at = @At("HEAD"))
    // IDEA thinks this is an error, it's not. Don't change it.
    private void onHeadDrawSprite(int x, int y, int z, int width, int height, Sprite sprite, CallbackInfo ci) {
        SpriteUtil.markSpriteActive(sprite);
    }
}
