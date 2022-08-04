package net.caffeinemc.sodium.mixin.core;

import com.mojang.blaze3d.systems.RenderSystem;
import net.caffeinemc.gfx.api.device.RenderConfiguration;
import net.caffeinemc.gfx.opengl.device.GlRenderDevice;
import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.interop.vanilla.pipeline.Blaze3DPipelineManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public class MixinRenderSystem {
    @Inject(method = "initRenderer", at = @At("TAIL"), remap = false)
    private static void sodium$setupDevice(int debugVerbosity, boolean debugSync, CallbackInfo ci) {
        SodiumClientMod.DEVICE = new GlRenderDevice(
                Blaze3DPipelineManager::new,
                new RenderConfiguration(
                        SodiumClientMod.options().advanced.enableApiDebug
                )
        );
    }
}
