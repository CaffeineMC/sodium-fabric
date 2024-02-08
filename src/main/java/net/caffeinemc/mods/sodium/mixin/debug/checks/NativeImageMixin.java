package net.caffeinemc.mods.sodium.mixin.debug.checks;

import com.mojang.blaze3d.platform.NativeImage;
import net.caffeinemc.mods.sodium.client.render.util.RenderAsserts;
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
