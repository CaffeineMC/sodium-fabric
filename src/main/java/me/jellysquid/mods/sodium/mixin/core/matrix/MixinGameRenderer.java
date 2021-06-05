package me.jellysquid.mods.sodium.mixin.core.matrix;

import me.jellysquid.mods.sodium.client.render.GameRendererContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Inject(method = "loadProjectionMatrix", at = @At("HEAD"))
    public void captureProjectionMatrix(Matrix4f matrix, CallbackInfo ci) {
        GameRendererContext.captureProjectionMatrix(matrix);
    }
}
