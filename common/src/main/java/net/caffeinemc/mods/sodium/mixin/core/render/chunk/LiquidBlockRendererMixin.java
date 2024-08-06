package net.caffeinemc.mods.sodium.mixin.core.render.chunk;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Apply after Fabric API
@Mixin(value = LiquidBlockRenderer.class, priority = 2000)
public class LiquidBlockRendererMixin {
    @Inject(method = "tesselate", at = @At("HEAD"), cancellable = true)
    private void onHeadRender(CallbackInfo ci) {
        if (FluidRenderer.renderFromVanilla()) {
            ci.cancel();
        }
    }
}
