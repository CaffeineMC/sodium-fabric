package net.caffeinemc.mods.sodium.mixin.debug.checks;

import com.mojang.blaze3d.vertex.BufferUploader;
import net.caffeinemc.mods.sodium.client.render.util.RenderAsserts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BufferUploader.class)
public class BufferRendererMixin {
    @Redirect(method = "drawWithShader", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;isOnRenderThreadOrInit()Z"))
    private static boolean validateCurrentThread$draw() {
        return RenderAsserts.validateCurrentThread();
    }
}
