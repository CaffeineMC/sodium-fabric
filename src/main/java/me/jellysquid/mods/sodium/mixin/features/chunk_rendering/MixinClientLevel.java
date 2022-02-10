package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import me.jellysquid.mods.sodium.render.SodiumLevelRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class MixinClientLevel {
    @Inject(method = "setLightReady", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk;setClientLightReady(Z)V", shift = At.Shift.AFTER))
    private void postLightUpdate(int chunkX, int chunkZ, CallbackInfo ci) {
        SodiumLevelRenderer.instance()
                .onChunkLightAdded(chunkX, chunkZ);
    }
}
