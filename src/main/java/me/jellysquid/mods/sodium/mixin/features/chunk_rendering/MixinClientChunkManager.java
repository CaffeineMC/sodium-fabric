package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListener;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListenerManager;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicReferenceArray;

@Mixin(ClientChunkManager.class)
public abstract class MixinClientChunkManager implements ChunkStatusListenerManager {
    @Shadow
    @Nullable
    public abstract WorldChunk getChunk(int i, int j, ChunkStatus chunkStatus, boolean bl);

    private final LongOpenHashSet loadedChunks = new LongOpenHashSet();
    private ChunkStatusListener listener;

    @Inject(method = "loadChunkFromPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;resetChunkColor(II)V", shift = At.Shift.AFTER))
    private void afterLoadChunkFromPacket(int x, int z, BiomeArray biomes, PacketByteBuf buf, CompoundTag tag, int verticalStripBitmask, boolean complete, CallbackInfoReturnable<WorldChunk> cir) {
        if (this.listener != null) {
            this.listener.onChunkAdded(x, z);
            this.loadedChunks.add(ChunkPos.toLong(x, z));
        }
    }

    @Inject(method = "unload", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientChunkManager$ClientChunkMap;compareAndSet(ILnet/minecraft/world/chunk/WorldChunk;Lnet/minecraft/world/chunk/WorldChunk;)Lnet/minecraft/world/chunk/WorldChunk;", shift = At.Shift.AFTER))
    private void afterUnloadChunk(int x, int z, CallbackInfo ci) {
        if (this.listener != null) {
            this.listener.onChunkRemoved(x, z);
            this.loadedChunks.remove(ChunkPos.toLong(x, z));
        }
    }

    @Inject(method = "updateLoadDistance", at = @At("RETURN"))
    private void afterLoadDistanceChanged(int loadDistance, CallbackInfo ci) {
        LongIterator it = this.loadedChunks.iterator();

        while (it.hasNext()) {
            long pos = it.nextLong();

            int x = ChunkPos.getPackedX(pos);
            int z = ChunkPos.getPackedZ(pos);

            if (this.getChunk(x, z, ChunkStatus.FULL, false) == null) {
                it.remove();

                if (this.listener != null) {
                    this.listener.onChunkRemoved(x, z);
                }
            }
        }
    }

    @Override
    public void setListener(ChunkStatusListener listener) {
        this.listener = listener;
    }

    @Mixin(targets = "net/minecraft/client/world/ClientChunkManager$ClientChunkMap")
    public static class MixinClientChunkMap {
        @Mutable
        @Shadow
        @Final
        private AtomicReferenceArray<WorldChunk> chunks;

        @Mutable
        @Shadow
        @Final
        private int diameter;

        @Mutable
        @Shadow
        @Final
        private int radius;

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
        private int getIndex(int chunkX, int chunkZ) {
            return (chunkZ & this.factor) * this.diameter + (chunkX & this.factor);
        }
    }
}
