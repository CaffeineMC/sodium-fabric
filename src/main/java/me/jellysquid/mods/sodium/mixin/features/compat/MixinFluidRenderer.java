package me.jellysquid.mods.sodium.mixin.features.compat;

import me.jellysquid.mods.sodium.client.compat.CompatHolder;
import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.client.texture.Sprite;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FluidRenderer.class)
public class MixinFluidRenderer {
    @Shadow
    @Final
    private Sprite[] waterSprites;
    @Shadow
    @Final
    private Sprite[] lavaSprites;

    @Inject(method = "onResourceReload", at = @At("RETURN"))
    public void onResourceReload(CallbackInfo ci) {
        CompatHolder.onFluidResourceReload(waterSprites, lavaSprites);
    }
}
