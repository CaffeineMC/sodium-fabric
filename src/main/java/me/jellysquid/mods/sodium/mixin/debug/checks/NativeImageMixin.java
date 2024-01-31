package me.jellysquid.mods.sodium.mixin.debug.checks;

import me.jellysquid.mods.sodium.client.render.util.RenderAsserts;
import net.minecraft.client.texture.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NativeImage.class)
public class NativeImageMixin {
    @Redirect(method = {
            "upload(IIIIIIIZZZZ)V"
    }, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;isOnRenderThreadOrInit()Z"))
    private boolean validateCurrentThread$upload() {
        return RenderAsserts.validateCurrentThread();
    }
}
