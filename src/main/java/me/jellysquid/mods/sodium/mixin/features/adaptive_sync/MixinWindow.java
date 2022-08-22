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
            if (!(GLFW.glfwExtensionSupported("GLX_EXT_swap_control_tear") || GLFW.glfwExtensionSupported("WGL_EXT_swap_control_tear"))) {
                SodiumClientMod.logger().warn("Tried to enable adaptive sync, but the system doesn't support it? Disabling.");
                SodiumClientMod.options().performance.useAdaptiveSync = false;
                GLFW.glfwSwapInterval(interval);
            } else {
                GLFW.glfwSwapInterval(interval == 1 ? -1 : 0);
            }
        } else {
            GLFW.glfwSwapInterval(interval);
        }
    }
}
