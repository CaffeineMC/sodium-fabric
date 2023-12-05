package me.jellysquid.mods.sodium.mixin.debug.checks;

import me.jellysquid.mods.sodium.client.render.util.RenderAsserts;
import net.minecraft.client.render.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(VertexFormat.class)
public class VertexFormatMixin {
    @Redirect(method = {
            "setupState",
            "clearState"
    }, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;isOnRenderThread()Z"))
    private boolean validateCurrentThread$modifyState() {
        return RenderAsserts.validateCurrentThread();
    }
}
