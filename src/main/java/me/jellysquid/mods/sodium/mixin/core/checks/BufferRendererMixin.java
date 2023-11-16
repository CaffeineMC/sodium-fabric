package me.jellysquid.mods.sodium.mixin.core.checks;

import me.jellysquid.mods.sodium.client.render.util.RenderAsserts;
import net.minecraft.client.render.BufferRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BufferRenderer.class)
public class BufferRendererMixin {
    @Redirect(method = "drawWithGlobalProgram", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;isOnRenderThreadOrInit()Z"))
    private static boolean validateCurrentThread$draw() {
        return RenderAsserts.validateCurrentThread();
    }
}
