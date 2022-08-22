package net.caffeinemc.sodium.mixin.features.chunk_rendering;

import net.caffeinemc.sodium.render.SodiumWorldRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {
    @Shadow @Final private MinecraftClient client;
    
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
    
    /**
     * This is done, so we don't have to reload the renderer immediately due to the view distance changing later in the
     * packet.
     */
    @Inject(method = "onGameJoin", at = @At("HEAD"))
    private void setViewDistanceEarly(GameJoinS2CPacket packet, CallbackInfo ci) {
        this.client.options.setServerViewDistance(packet.viewDistance());
    }
}
