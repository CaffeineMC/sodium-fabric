package me.jellysquid.mods.sodium.mixin.workarounds.context_creation;

import me.jellysquid.mods.sodium.client.util.workarounds.DriverWorkarounds;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(Window.class)
public class MixinWindow {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J"))
    private long wrapGlfwCreateWindow(int width, int height, CharSequence title, long monitor, long share) {
        DriverWorkarounds.beforeContextCreation();

        try {
            return GLFW.glfwCreateWindow(width, height, title, monitor, share);
        } finally {
            DriverWorkarounds.afterContextCreation();
        }
    }
}
