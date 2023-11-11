package me.jellysquid.mods.sodium.mixin.core.checks;

import com.mojang.blaze3d.platform.GlStateManager;
import me.jellysquid.mods.sodium.client.render.util.RenderAsserts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GlStateManager.class)
public class GlStateManagerMixin {
    @Redirect(method = {
            "upload",
    }, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;isOnRenderThreadOrInit()Z"), remap = false)
    private static boolean validateCurrentThread$upload() {
        return RenderAsserts.validateCurrentThread();
    }
}
