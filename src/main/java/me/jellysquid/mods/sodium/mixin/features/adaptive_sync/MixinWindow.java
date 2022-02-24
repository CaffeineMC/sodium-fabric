package me.jellysquid.mods.sodium.mixin.features.adaptive_sync;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Window.class)
public class MixinWindow {
    @Redirect(method = "setVsync", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapInterval(I)V"))
    private void setSwapInterval(int interval) {
        if (SodiumClientMod.options().performance.useAdaptiveSync) {
            GLFW.glfwSwapInterval(interval == 1 ? -1 : 0);
        } else {
            GLFW.glfwSwapInterval(interval);
        }
    }
}
