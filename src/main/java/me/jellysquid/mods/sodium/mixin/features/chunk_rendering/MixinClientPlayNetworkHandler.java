package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {
    @Inject(method = "onChunkData", at = @At("RETURN"))
    private void postLoadChunk(ChunkDataS2CPacket packet, CallbackInfo ci) {
        SodiumWorldRenderer.instance()
                .onChunkAdded(packet.getX(), packet.getZ());
    }

    @Inject(method = "onUnloadChunk", at = @At("RETURN"))
    private void postUnloadChunk(UnloadChunkS2CPacket packet, CallbackInfo ci) {
        SodiumWorldRenderer.instance()
                .onChunkRemoved(packet.getX(), packet.getZ());
    }
}
