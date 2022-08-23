package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicReferenceArray;

@Mixin(targets = "net/minecraft/client/world/ClientChunkManager$ClientChunkMap")
public class MixinClientChunkMap {
    @Mutable
    @Shadow
    @Final
    AtomicReferenceArray<WorldChunk> chunks;

    @Mutable
    @Shadow
    @Final
    private int diameter;

    @Mutable
    @Shadow
    @Final
    int radius;

    private int factor;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void reinit(ClientChunkManager outer, int loadDistance, CallbackInfo ci) {
        // This re-initialization is a bit expensive on memory, but it only happens when either the world is
        // switched or the render distance is changed;
        this.radius = loadDistance;

        // Make the diameter a power-of-two so we can exploit bit-wise math when computing indices
        this.diameter = MathHelper.smallestEncompassingPowerOfTwo(loadDistance * 2 + 1);

        // The factor is used as a bit mask to replace the modulo in getIndex
        this.factor = this.diameter - 1;

        this.chunks = new AtomicReferenceArray<>(this.diameter * this.diameter);
    }

    /**
     * @reason Avoid expensive modulo
     * @author JellySquid
     */
    @Overwrite
    int getIndex(int chunkX, int chunkZ) {
        return (chunkZ & this.factor) * this.diameter + (chunkX & this.factor);
    }
}
