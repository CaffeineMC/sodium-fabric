package net.caffeinemc.sodium.mixin.core;

import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import net.caffeinemc.gfx.api.sync.Fence;
import net.caffeinemc.sodium.SodiumClientMod;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {
    private final PriorityQueue<Fence> fences = new ObjectArrayFIFOQueue<>();

    @Inject(method = "render", at = @At("HEAD"))
    private void preRender(boolean tick, CallbackInfo ci) {
        while (this.fences.size() > SodiumClientMod.options().advanced.cpuRenderAheadLimit) {
            var fence = this.fences.dequeue();
            fence.sync(true);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void postRender(boolean tick, CallbackInfo ci) {
        this.fences.enqueue(SodiumClientMod.DEVICE.createFence());
    }
}
