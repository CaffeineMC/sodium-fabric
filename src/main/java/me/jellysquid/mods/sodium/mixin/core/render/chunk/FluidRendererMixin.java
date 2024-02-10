package me.jellysquid.mods.sodium.mixin.core.render.chunk;

import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Apply after Fabric API
@Mixin(value = net.minecraft.client.render.block.FluidRenderer.class, priority = 2000)
public class FluidRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onHeadRender(CallbackInfo ci) {
        if (FluidRenderer.renderFromVanilla()) {
            ci.cancel();
        }
    }
}
