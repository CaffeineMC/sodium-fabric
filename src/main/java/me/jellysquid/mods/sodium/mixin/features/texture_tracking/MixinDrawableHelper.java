package me.jellysquid.mods.sodium.mixin.features.texture_tracking;

import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrawableHelper.class)
public class MixinDrawableHelper {
    @Inject(method = "drawSprite(Lnet/minecraft/client/util/math/MatrixStack;IIIIILnet/minecraft/client/texture/Sprite;)V\n", at = @At("HEAD"))
    private static void onHeadDrawSprite(MatrixStack matrices, int x, int y, int z, int width, int height, Sprite sprite, CallbackInfo ci) {
        SpriteUtil.markSpriteActive(sprite);
    }
}
