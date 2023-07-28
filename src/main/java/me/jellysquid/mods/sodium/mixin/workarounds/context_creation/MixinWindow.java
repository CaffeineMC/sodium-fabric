package me.jellysquid.mods.sodium.mixin.workarounds.context_creation;

import me.jellysquid.mods.sodium.client.util.workarounds.Workarounds;
import me.jellysquid.mods.sodium.client.util.workarounds.driver.nvidia.NvidiaWorkarounds;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(Window.class)
public class MixinWindow {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J"))
    private long wrapGlfwCreateWindow(int width, int height, CharSequence title, long monitor, long share) {
        if (Workarounds.isWorkaroundEnabled(Workarounds.Reference.NVIDIA_THREADED_OPTIMIZATIONS)) {
            NvidiaWorkarounds.install();
        }

        try {
            return GLFW.glfwCreateWindow(width, height, title, monitor, share);
        } finally {
            if (Workarounds.isWorkaroundEnabled(Workarounds.Reference.NVIDIA_THREADED_OPTIMIZATIONS)) {
                NvidiaWorkarounds.uninstall();
            }
        }
    }
}
