package me.jellysquid.mods.sodium.mixin.core;

import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import me.jellysquid.mods.sodium.SodiumClientMod;
import me.jellysquid.mods.sodium.gui.screen.UserConfigErrorScreen;
import me.jellysquid.mods.sodium.opengl.device.RenderDevice;
import me.jellysquid.mods.sodium.opengl.sync.Fence;
import me.jellysquid.mods.sodium.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.render.immediate.RenderImmediate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraftClient {
    private final PriorityQueue<Fence> fences = new ObjectArrayFIFOQueue<>();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void postInit(GameConfig args, CallbackInfo ci) {
        if (SodiumClientMod.options().isReadOnly()) {
            var parent = Minecraft.getInstance().screen;
            Minecraft.getInstance().setScreen(new UserConfigErrorScreen(() -> parent));
        }
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    private void preRender(boolean tick, CallbackInfo ci) {
        while (this.fences.size() > SodiumClientMod.options().advanced.cpuRenderAheadLimit) {
            var fence = this.fences.dequeue();
            fence.sync();
        }
    }

    @Inject(method = "runTick", at = @At("RETURN"))
    private void postRender(boolean tick, CallbackInfo ci) {
        this.fences.enqueue(RenderDevice.INSTANCE.createFence());

        RenderImmediate.tryFlush();
        var instance = SodiumWorldRenderer.instanceNullable();

        if (instance != null) {
            instance.flush();
        }
    }
}
