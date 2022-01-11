package me.jellysquid.mods.sodium.mixin.core;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.WindowSettings;
import net.minecraft.client.util.MonitorTracker;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class MixinWindow {
    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J", shift = At.Shift.BEFORE))
    private void modifyRequestedContext(WindowEventHandler eventHandler, MonitorTracker monitorTracker, WindowSettings settings, String videoMode, String title, CallbackInfo ci) {
        if (System.getProperty("sodium.skipContextRequest", "false").equals("true")) {
            return;
        }

        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 6);
        GLFW.glfwSetErrorCallback(MixinWindow::sodium$throwGlError);
    }

    private static void sodium$throwGlError(int error, long description) {
        RenderSystem.assertInInitPhase();
        String string = "GLFW error " + error + ": " + MemoryUtil.memUTF8(description);
        TinyFileDialogs.tinyfd_messageBox("Minecraft",
                string +
                        "\n\nSodium requires a graphics card driver with support for OpenGL 4.6 Core." +
                        "\nPlease make sure you have up-to-date drivers (see aka.ms/mcdriver for instructions).",
                "ok",
                "error",
                false);
        throw new RuntimeException(string);
    }
}
