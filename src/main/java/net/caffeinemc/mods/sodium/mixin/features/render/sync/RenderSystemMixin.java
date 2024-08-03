package net.caffeinemc.mods.sodium.mixin.features.render.sync;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = RenderSystem.class, remap = false)
public class RenderSystemMixin {
    /**
     * @author theyareonit
     * @reason Improve frame synchronization
     */
    @Overwrite
    public static void limitDisplayFPS(int fps) {
        double frameTime = 1.0 / fps;
        double now = GLFW.glfwGetTime();
        double end = (now - (now % frameTime)) + frameTime; // subtracting (now % frameTime) corrects for desync

        for (; now < end; now = GLFW.glfwGetTime()) {
            double waitTime = (end - now) - 0.002; // -2ms to account for sleep imprecision on some operating systems
            if (waitTime >= 0.001) { // cant sleep less than 1ms without platform-specific code
                GLFW.glfwWaitEventsTimeout(waitTime);
            }
        }

        GLFW.glfwPollEvents();
    }
}