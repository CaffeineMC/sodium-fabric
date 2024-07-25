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
        double frametime = 1.0 / fps;
        double now = GLFW.glfwGetTime();
        double target = (now - (now % frametime)) + frametime; // subtracting (now % frametime) corrects for desync

        for (; now < target; now = GLFW.glfwGetTime()) {
            int waitTime = (int)((target - now) * 1000.0) - 2; // -2ms to account for sleep imprecision in some OSes
            if (waitTime >= 1) { // cant sleep less than 1ms without platform-specific code
                try {
                    Thread.sleep(waitTime); // precision seems fine on both linux and windows
                }
                catch (Exception e) {}
            }
        }

        GLFW.glfwPollEvents(); // glfwWaitEventsTimeout won't catch every input if subtracting 2ms from the timeout
    }
}