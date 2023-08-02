package me.jellysquid.mods.sodium.mixin.core.world.map;

import me.jellysquid.mods.sodium.client.render.chunk.map.ChunkStatus;
import me.jellysquid.mods.sodium.client.render.chunk.map.ChunkTrackerHolder;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.LightData;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Shadow
    private ClientWorld world;

    @Inject(
            method = "readLightData",
            at = @At("RETURN")
    )
    private void onLightDataReceived(int x, int z, LightData data, CallbackInfo ci) {
        ChunkTrackerHolder.get(this.world)
                .onChunkStatusAdded(x, z, ChunkStatus.FLAG_HAS_LIGHT_DATA);
    }

    @Inject(method = "onUnloadChunk", at = @At("RETURN"))
    private void onChunkUnloadPacket(UnloadChunkS2CPacket packet, CallbackInfo ci) {
        ChunkTrackerHolder.get(this.world)
                .onChunkStatusRemoved(packet.getX(), packet.getZ(), ChunkStatus.FLAG_ALL);
    }
}
