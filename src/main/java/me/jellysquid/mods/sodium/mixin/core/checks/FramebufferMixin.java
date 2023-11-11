package me.jellysquid.mods.sodium.mixin.core.checks;

import me.jellysquid.mods.sodium.client.render.util.RenderAsserts;
import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Framebuffer.class)
public class FramebufferMixin {
    @Redirect(method = {
            "resize",
            "beginWrite",
            "endWrite",
    }, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;isOnRenderThread()Z"))
    private boolean validateCurrentThread$imageOperations() {
        return RenderAsserts.validateCurrentThread();
    }

    @Redirect(method = {
            "draw(IIZ)V",
    }, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;isInInitPhase()Z"))
    private boolean validateCurrentThread$draw() {
        return RenderAsserts.validateCurrentThread();
    }
}
