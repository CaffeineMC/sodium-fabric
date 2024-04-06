package net.caffeinemc.mods.sodium.mixin.core;

import com.mojang.blaze3d.platform.Window;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.Workarounds;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Window.class)
public class WindowMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J"), require = 0)
    public long setAdditionalWindowHints(int width, int height, CharSequence title, long monitor, long share) {
        if (SodiumClientMod.options().performance.useNoErrorGLContext &&
                !Workarounds.isWorkaroundEnabled(Workarounds.Reference.NO_ERROR_CONTEXT_UNSUPPORTED)) {
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_NO_ERROR, GLFW.GLFW_TRUE);
        }
        return GLFW.glfwCreateWindow(width, height, title, monitor, share);
    }
}
