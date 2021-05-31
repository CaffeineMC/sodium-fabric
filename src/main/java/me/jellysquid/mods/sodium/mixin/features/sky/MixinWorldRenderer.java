package me.jellysquid.mods.sodium.mixin.features.sky;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import me.jellysquid.mods.sodium.client.gl.util.GlFogHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {
    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    private void preRenderSky(MatrixStack matrices, float tickDelta, CallbackInfo callbackInfo) {
        // Prevents the sky layer from rendering when the fog distance is reduced from the default.
        // This helps prevent situations where the sky is visible through chunks culled by fog occlusion.
        if (MinecraftClient.getInstance().gameRenderer.getViewDistance() != GlFogHelper.getFogCutoff()) {
            callbackInfo.cancel();
        }
    }
}
