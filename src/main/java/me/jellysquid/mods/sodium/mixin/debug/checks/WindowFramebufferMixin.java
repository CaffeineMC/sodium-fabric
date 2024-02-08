package me.jellysquid.mods.sodium.mixin.debug.checks;

import com.mojang.blaze3d.pipeline.MainTarget;
import me.jellysquid.mods.sodium.client.render.util.RenderAsserts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MainTarget.class)
public class WindowFramebufferMixin {
    @Redirect(method = {
            "<init>"
    }, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;isOnRenderThread()Z"))
    private boolean validateCurrentThread$init() {
        return RenderAsserts.validateCurrentThread();
    }
}
