package me.jellysquid.mods.sodium.mixin.no_hurt_cam;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;

import me.jellysquid.mods.sodium.client.SodiumClientMod;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Inject(method="bobViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void cancelHurtCam(MatrixStack ms, float f, CallbackInfo ci)
    {
        if(!SodiumClientMod.options().quality.enableHurtCam)
        {
            ci.cancel();
        }
    }
}