package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import me.jellysquid.mods.sodium.client.render.chunk.ChunkStatus;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkTracker;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public class MixinClientWorld {
    @Shadow
    @Final
    private ClientChunkManager chunkManager;

    @Inject(method = "markChunkRenderability", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/WorldChunk;setShouldRenderOnUpdate(Z)V", shift = At.Shift.AFTER))
    private void postLightUpdate(int chunkX, int chunkZ, CallbackInfo ci) {
        var tracker = ChunkTracker.from(this.chunkManager);
        tracker.mark(chunkX, chunkZ, ChunkStatus.FLAG_HAS_LIGHT_DATA);
    }
}
