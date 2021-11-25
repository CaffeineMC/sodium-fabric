package me.jellysquid.mods.sodium.mixin.core;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.screen.ConfigCorruptedScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import org.lwjgl.opengl.GL32C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {
    private final LongArrayFIFOQueue fences = new LongArrayFIFOQueue();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void postInit(RunArgs args, CallbackInfo ci) {
        if (SodiumClientMod.options().isReadOnly()) {
            var parent = MinecraftClient.getInstance().currentScreen;
            MinecraftClient.getInstance().setScreen(new ConfigCorruptedScreen(() -> parent));
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void preRender(boolean tick, CallbackInfo ci) {
        while (this.fences.size() > SodiumClientMod.options().advanced.cpuRenderAheadLimit) {
            var fence = this.fences.dequeueLong();
            GL32C.glClientWaitSync(fence, GL32C.GL_SYNC_FLUSH_COMMANDS_BIT, Long.MAX_VALUE);
            GL32C.glDeleteSync(fence);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void postRender(boolean tick, CallbackInfo ci) {
        var fence = GL32C.glFenceSync(GL32C.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);

        if (fence == 0) {
            throw new RuntimeException("Failed to create fence object");
        }

        this.fences.enqueue(fence);
    }
}
