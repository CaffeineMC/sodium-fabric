package me.jellysquid.mods.sodium.mixin.core;

import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import me.jellysquid.mods.sodium.SodiumClient;
import me.jellysquid.mods.sodium.SodiumRender;
import me.jellysquid.mods.thingl.device.RenderDevice;
import me.jellysquid.mods.thingl.sync.Fence;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL32C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {
    private final PriorityQueue<Fence> fences = new ObjectArrayFIFOQueue<>();

    @Inject(method = "render", at = @At("HEAD"))
    private void preRender(boolean tick, CallbackInfo ci) {
        while (this.fences.size() > SodiumClient.options().advanced.maxPreRenderedFrames) {
            var fence = this.fences.dequeue();
            fence.sync();

            SodiumRender.DEVICE.deleteFence(fence);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void postRender(boolean tick, CallbackInfo ci) {
        this.fences.enqueue(SodiumRender.DEVICE.createFence());
    }
}
