package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import me.jellysquid.mods.sodium.client.render.chunk.ChunkStatus;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkTracker;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(ClientChunkManager.class)
public class MixinClientChunkManager implements ChunkTracker.Accessor {
    private ChunkTracker tracker;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(ClientWorld world, int loadDistance, CallbackInfo ci) {
        this.tracker = new ChunkTracker(loadDistance);
    }

    @Inject(method = "setChunkMapCenter", at = @At("HEAD"))
    private void onMapCenterChanged(int x, int z, CallbackInfo ci) {
        this.tracker.updateCenter(x, z);
    }

    @Inject(method = "updateLoadDistance", at = @At("HEAD"))
    private void onMapRadiusChanged(int loadDistance, CallbackInfo ci) {
        this.tracker.updateLoadDistance(loadDistance);
    }

    @Inject(method = "loadChunkFromPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;resetChunkColor(Lnet/minecraft/util/math/ChunkPos;)V", shift = At.Shift.AFTER))
    private void onChunkLoaded(int x, int z, PacketByteBuf buf, NbtCompound nbt, Consumer<ChunkData.BlockEntityVisitor> consumer, CallbackInfoReturnable<@Nullable WorldChunk> cir) {
        this.tracker.mark(x, z, ChunkStatus.FLAG_HAS_BLOCK_DATA);
    }

    @Inject(method = "unload", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientChunkManager$ClientChunkMap;compareAndSet(ILnet/minecraft/world/chunk/WorldChunk;Lnet/minecraft/world/chunk/WorldChunk;)Lnet/minecraft/world/chunk/WorldChunk;", shift = At.Shift.AFTER))
    private void onUnloadChunk(int x, int z, CallbackInfo ci) {
        this.tracker.remove(x, z);
    }

    @Override
    public ChunkTracker getTracker() {
        return this.tracker;
    }
}