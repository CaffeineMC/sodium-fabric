package net.caffeinemc.sodium.mixin.core;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import net.caffeinemc.gfx.api.device.RenderConfiguration;
import net.caffeinemc.gfx.api.sync.Fence;
import net.caffeinemc.gfx.opengl.device.GlRenderDevice;
import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.interop.vanilla.pipeline.Blaze3DPipelineManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public class MixinRenderSystem {
    private static final PriorityQueue<Fence> fences = new ObjectArrayFIFOQueue<>();
    
    @Redirect(method = "flipFrame", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwPollEvents()V", ordinal = 0))
    private static void removeFirstPoll() {
        // noop
    }
    
    @Inject(method = "flipFrame", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V", shift = At.Shift.BEFORE))
    private static void preFlip(long window, CallbackInfo ci) {
        while (fences.size() > SodiumClientMod.options().advanced.cpuRenderAheadLimit) {
            var fence = fences.dequeue();
            fence.sync(true);
        }
    }
    
    @Inject(method = "flipFrame", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V"))
    private static void postFlip(long window, CallbackInfo ci) {
        fences.enqueue(SodiumClientMod.DEVICE.createFence());
    }
    
    @Inject(method = "initRenderer", at = @At("TAIL"), remap = false)
    private static void sodium$setupDevice(int debugVerbosity, boolean debugSync, CallbackInfo ci) {
        SodiumClientMod.DEVICE = new GlRenderDevice(
                Blaze3DPipelineManager::new,
                new RenderConfiguration(
                        SodiumClientMod.options().advanced.enableApiDebug
                )
        );
    }
}
