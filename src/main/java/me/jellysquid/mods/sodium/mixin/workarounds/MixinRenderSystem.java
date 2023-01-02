package me.jellysquid.mods.sodium.mixin.workarounds;

import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.util.workarounds.DriverWorkarounds;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public class MixinRenderSystem {
    @Inject(method = "initRenderThread", at = @At("RETURN"), remap = false)
    private static void postInitRenderThread(CallbackInfo ci) {
        DriverWorkarounds.init();
    }
}
