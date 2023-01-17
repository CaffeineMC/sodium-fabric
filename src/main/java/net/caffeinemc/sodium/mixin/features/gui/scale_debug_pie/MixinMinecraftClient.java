package net.caffeinemc.sodium.mixin.features.gui.scale_debug_pie;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {
    @Shadow
    @Final
    private Window window;

    @Redirect(method = "drawProfilerResults", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;setOrtho(FFFFFF)Lorg/joml/Matrix4f;"))
    private Matrix4f getScaledPosition(Matrix4f instance, float left, float right, float bottom, float top, float zNear, float zFar) {
        return instance.setOrtho(
                left,
                (float) (window.getFramebufferWidth() / (window.getScaleFactor() / 2)),
                (float) (window.getFramebufferHeight() / (window.getScaleFactor() / 2)),
                top,
                zNear,
                zFar
        );
    }

    @Redirect(method = "drawProfilerResults", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;getFramebufferWidth()I", ordinal = 1))
    private int getScaledWidth(Window instance) {
        return (int) (window.getFramebufferWidth() / (window.getScaleFactor() / 2));
    }

    @Redirect(method = "drawProfilerResults", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;getFramebufferHeight()I", ordinal = 1))
    private int getScaledHeight(Window instance) {
        return (int) (window.getFramebufferHeight() / (window.getScaleFactor() / 2));
    }
}
