package me.jellysquid.mods.sodium.mixin.debug.checks;

import me.jellysquid.mods.sodium.client.render.util.RenderAsserts;
import net.minecraft.client.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractTexture.class)
public class AbstractTextureMixin {
    @Redirect(method = {
            "clearGlId"
    }, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;isOnRenderThread()Z"))
    private boolean validateCurrentThread$cleanup() {
        return RenderAsserts.validateCurrentThread();
    }

    @Redirect(method = {
            "bindTexture"
    }, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;isOnRenderThreadOrInit()Z"))
    private boolean validateCurrentThread$bind() {
        return RenderAsserts.validateCurrentThread();
    }
}
