package me.jellysquid.mods.sodium.mixin.features.sky;

import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {
    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    private void preRenderSky(MatrixStack matrices, Matrix4f matrix4f, float tickDelta, Runnable runnable, CallbackInfo callbackInfo) {
        // Prevents the sky layer from rendering when the fog distance is reduced from the default.
        // This helps prevent situations where the sky is visible through chunks culled by fog occlusion.
        if (Math.max(MinecraftClient.getInstance().gameRenderer.getViewDistance() - 16.0F, 32.0F) != RenderSystem.getShaderFogEnd()) {
            callbackInfo.cancel();
        }
    }
}
