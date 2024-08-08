package net.caffeinemc.mods.sodium.mixin.debug.checks;

import net.caffeinemc.mods.sodium.client.render.util.RenderAsserts;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SpriteContents.Ticker.class)
public class SpriteContentsAnimatorImplMixin {
    @Redirect(method = {
            "tickAndUpload"
    }, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;isOnRenderThread()Z"))
    private boolean validateCurrentThread$tick() {
        return RenderAsserts.validateCurrentThread();
    }
}
