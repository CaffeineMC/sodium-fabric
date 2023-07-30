package me.jellysquid.mods.sodium.mixin.features.options.render_layers;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.render.RenderLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderLayers.class)
public class RenderLayersMixin {
    @Unique
    private static boolean leavesFancy;

    @Redirect(
            method = { "getBlockLayer", "getMovingBlockLayer" },
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/RenderLayers;fancyGraphicsOrBetter:Z"))
    private static boolean redirectLeavesShouldBeFancy() {
        return leavesFancy;
    }

    @Inject(method = "setFancyGraphicsOrBetter", at = @At("RETURN"))
    private static void onSetFancyGraphicsOrBetter(boolean fancyGraphicsOrBetter, CallbackInfo ci) {
        leavesFancy = SodiumClientMod.options().quality.leavesQuality.isFancy(fancyGraphicsOrBetter ? GraphicsMode.FANCY : GraphicsMode.FAST);
    }
}
