package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicReferenceArray;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.util.Mth;
import net.minecraft.world.level.chunk.LevelChunk;

@Mixin(targets = "net/minecraft/client/multiplayer/ClientChunkCache$Storage")
public class MixinClientChunkCacheStorage {
    @Mutable
    @Shadow
    @Final
    AtomicReferenceArray<LevelChunk> chunks;

    @Mutable
    @Shadow
    @Final
    private int viewRange;

    @Mutable
    @Shadow
    @Final
    int chunkRadius;

    private int factor;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void reinit(ClientChunkCache outer, int loadDistance, CallbackInfo ci) {
        // This re-initialization is a bit expensive on memory, but it only happens when either the world is
        // switched or the render distance is changed;
        this.chunkRadius = loadDistance;

        // Make the diameter a power-of-two so we can exploit bit-wise math when computing indices
        this.viewRange = Mth.smallestEncompassingPowerOfTwo(loadDistance * 2 + 1);

        // The factor is used as a bit mask to replace the modulo in getIndex
        this.factor = this.viewRange - 1;

        this.chunks = new AtomicReferenceArray<>(this.viewRange * this.viewRange);
    }

    /**
     * @reason Avoid expensive modulo
     * @author JellySquid
     */
    @Overwrite
    int getIndex(int chunkX, int chunkZ) {
        return (chunkZ & this.factor) * this.viewRange + (chunkX & this.factor);
    }
}
