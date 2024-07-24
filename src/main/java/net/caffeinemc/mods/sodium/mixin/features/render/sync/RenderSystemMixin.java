package net.caffeinemc.mods.sodium.mixin.features.render.sync;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RenderSystem.class, remap = false)
public class RenderSystemMixin {

    @Shadow
    private static double lastDrawTime = Double.MIN_VALUE;

    /**
     * @author theyareonit
     * @reason Improve frame synchronization
     */
    @Overwrite
    public static void limitDisplayFPS(int fps) {
        double frametime = 1.0 / fps;
        double target =  lastDrawTime + frametime;
        double now = GLFW.glfwGetTime();

        for (; now < target; now = GLFW.glfwGetTime()) {
            double waitTime = (target - now) - 0.002; // -2ms to account for inaccuracy of timeouts on some operating systems
            if (waitTime >= 0.001) { // pretty sure you can't sleep for less than 1ms on Windows or Linux
                GLFW.glfwWaitEventsTimeout(waitTime); // could be replaced with Thread.sleep(), but i'm not sure if it'd be as precise
            }
        }

        lastDrawTime = now - (now % frametime); // subtracting (now % frametime) keeps frames in sync
        GLFW.glfwPollEvents(); // glfwWaitEventsTimeout won't catch every input if subtracting 2ms from the timeout
    }
}