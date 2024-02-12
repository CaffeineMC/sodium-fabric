package net.caffeinemc.mods.sodium.mixin.features.options.render_layers;

import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemBlockRenderTypes.class)
public class RenderLayersMixin {
    @Unique
    private static boolean leavesFancy;

    @Redirect(
            method = { "getChunkRenderType", "getMovingBlockRenderType" },
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/ItemBlockRenderTypes;renderCutout:Z"))
    private static boolean redirectLeavesShouldBeFancy() {
        return leavesFancy;
    }

    @Inject(method = "setFancy", at = @At("RETURN"))
    private static void onSetFancyGraphicsOrBetter(boolean fancyGraphicsOrBetter, CallbackInfo ci) {
        leavesFancy = SodiumClientMod.options().quality.leavesQuality.isFancy(fancyGraphicsOrBetter ? GraphicsStatus.FANCY : GraphicsStatus.FAST);
    }
}
