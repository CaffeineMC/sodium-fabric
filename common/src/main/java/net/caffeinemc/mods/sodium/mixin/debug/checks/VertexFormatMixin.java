package net.caffeinemc.mods.sodium.mixin.debug.checks;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.caffeinemc.mods.sodium.client.render.util.RenderAsserts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(VertexFormat.class)
public class VertexFormatMixin {
    @Redirect(method = {
            "setupBufferState",
            "clearBufferState"
    }, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;isOnRenderThread()Z"))
    private boolean validateCurrentThread$modifyState() {
        return RenderAsserts.validateCurrentThread();
    }
}
