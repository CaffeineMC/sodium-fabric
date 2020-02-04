package me.jellysquid.mods.sodium.mixin.render.static_fov;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Inject(method = "getFov", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;lastMovementFovMultiplier:F"), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private void onRequestFov(Camera camera, float deltaTime, boolean changingFov, CallbackInfoReturnable<Double> cir, double currentFov){
        if(SodiumClientMod.options().quality.staticFov){
            cir.setReturnValue(currentFov);
        }
    }

}
