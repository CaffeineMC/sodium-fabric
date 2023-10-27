package me.jellysquid.mods.sodium.mixin.core;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.util.workarounds.Workarounds;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowMixin {

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V", remap = false))
    private void windowHintOverride(int hint, int value) {
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 6);
    }
    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J", shift = At.Shift.BEFORE))
    public void setAdditionalWindowHints(CallbackInfo ci) {
        if (SodiumClientMod.options().performance.useNoErrorGLContext &&
                !Workarounds.isWorkaroundEnabled(Workarounds.Reference.NO_ERROR_CONTEXT_UNSUPPORTED)) {
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_NO_ERROR, GLFW.GLFW_TRUE);
        }
    }
}
