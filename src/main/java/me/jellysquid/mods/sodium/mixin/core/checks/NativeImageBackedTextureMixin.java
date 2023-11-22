package me.jellysquid.mods.sodium.mixin.core.checks;

import me.jellysquid.mods.sodium.client.render.util.RenderAsserts;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NativeImageBackedTexture.class)
public class NativeImageBackedTextureMixin {
    @Redirect(method = "<init>(Lnet/minecraft/client/texture/NativeImage;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;isOnRenderThread()Z"))
    private boolean validateCurrentThread$init() {
        return RenderAsserts.validateCurrentThread();
    }
}
