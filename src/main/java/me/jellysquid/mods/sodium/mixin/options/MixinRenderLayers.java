package me.jellysquid.mods.sodium.mixin.options;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.client.render.RenderLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderLayers.class)
public class MixinRenderLayers {
    private static boolean useFancyLeaves = true;

    @Redirect(method = "getBlockLayer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/RenderLayers;fancyGraphics:Z"))
    private static boolean redirectGetLeavesQuality() {
        return useFancyLeaves;
    }

    @Inject(method = "setFancyGraphics", at = @At("RETURN"))
    private static void onFlagsUpdated(boolean fancyGraphics, CallbackInfo ci) {
        useFancyLeaves = SodiumClientMod.options().quality.leavesQuality.isFancy(fancyGraphics);
    }
}
