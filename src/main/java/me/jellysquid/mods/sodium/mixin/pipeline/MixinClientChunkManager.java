package me.jellysquid.mods.sodium.mixin.pipeline;

import me.jellysquid.mods.sodium.client.render.chunk.ExtendedClientChunkManager;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListener;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientChunkManager.class)
public class MixinClientChunkManager implements ExtendedClientChunkManager {
    private ChunkStatusListener listener;

    @Inject(method = "loadChunkFromPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientChunkManager$ClientChunkMap;set(ILnet/minecraft/world/chunk/WorldChunk;)V"))
    private void onChunkLoaded(int i, int j, BiomeArray biomeArray, PacketByteBuf packetByteBuf, CompoundTag compoundTag, int k, CallbackInfoReturnable<WorldChunk> cir) {
        if (this.listener != null) {
            this.listener.onChunkAdded(i, j);
        }
    }

    @Inject(method = "unload", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientChunkManager$ClientChunkMap;compareAndSet(ILnet/minecraft/world/chunk/WorldChunk;Lnet/minecraft/world/chunk/WorldChunk;)Lnet/minecraft/world/chunk/WorldChunk;"))
    private void onChunkUnloaded(int chunkX, int chunkZ, CallbackInfo ci) {
        if (this.listener != null) {
            this.listener.onChunkRemoved(chunkX, chunkZ);
        }
    }

    @Override
    public void setListener(ChunkStatusListener listener) {
        this.listener = listener;
    }
}
