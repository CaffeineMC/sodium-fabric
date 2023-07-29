package me.jellysquid.mods.sodium.mixin.core.world.map;

import me.jellysquid.mods.sodium.client.render.chunk.map.ChunkStatus;
import me.jellysquid.mods.sodium.client.render.chunk.map.ChunkTrackerHolder;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.light.LightingProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.BitSet;
import java.util.Iterator;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Shadow
    private ClientWorld world;

    @Inject(
            method = "updateLighting",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/chunk/light/LightingProvider;enqueueSectionData(Lnet/minecraft/world/LightType;Lnet/minecraft/util/math/ChunkSectionPos;Lnet/minecraft/world/chunk/ChunkNibbleArray;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onLightDataReceived(int chunkX, int chunkZ, LightingProvider provider, LightType type, BitSet inited, BitSet uninited, Iterator<byte[]> nibbles, CallbackInfo ci) {
        ChunkTrackerHolder.get(this.world)
                .onChunkStatusAdded(chunkX, chunkZ, ChunkStatus.FLAG_HAS_LIGHT_DATA);
    }

    @Inject(method = "onUnloadChunk", at = @At("RETURN"))
    private void onChunkUnloadPacket(UnloadChunkS2CPacket packet, CallbackInfo ci) {
        ChunkTrackerHolder.get(this.world)
                .onChunkStatusRemoved(packet.getX(), packet.getZ(), ChunkStatus.FLAG_ALL);
    }
}
