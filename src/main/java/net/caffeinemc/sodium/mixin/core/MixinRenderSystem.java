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
        // should fix some bugs with minecraft polling events twice for some reason (why does it do that in the first place?)
    }
    
    @Inject(method = "flipFrame", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V"))
    private static void postFlip(long window, CallbackInfo ci) {
        fences.enqueue(SodiumClientMod.DEVICE.createFence());
        
        while (fences.size() > SodiumClientMod.options().advanced.cpuRenderAheadLimit) {
            var fence = fences.dequeue();
            fence.sync(true);
        }
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
