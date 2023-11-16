package me.jellysquid.mods.sodium.mixin.core.checks;

import me.jellysquid.mods.sodium.client.render.util.RenderAsserts;
import net.minecraft.client.texture.SpriteAtlasTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SpriteAtlasTexture.class)
public class SpriteAtlasTextureMixin {
    @Redirect(method = {
            "tick"
    }, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;isOnRenderThread()Z"))
    private boolean validateCurrentThread$tick() {
        return RenderAsserts.validateCurrentThread();
    }
}
