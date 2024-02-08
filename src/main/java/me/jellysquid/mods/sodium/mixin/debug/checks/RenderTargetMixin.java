package me.jellysquid.mods.sodium.mixin.debug.checks;

import com.mojang.blaze3d.pipeline.RenderTarget;
import me.jellysquid.mods.sodium.client.render.util.RenderAsserts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderTarget.class)
public class RenderTargetMixin {
    @Redirect(method = {
            "resize",
            "bindWrite",
            "unbindWrite",
    }, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;isOnRenderThread()Z"))
    private boolean validateCurrentThread$imageOperations() {
        return RenderAsserts.validateCurrentThread();
    }

    @Redirect(method = {
            "blitToScreen(IIZ)V",
    }, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;isInInitPhase()Z"))
    private boolean validateCurrentThread$draw() {
        return RenderAsserts.validateCurrentThread();
    }
}
