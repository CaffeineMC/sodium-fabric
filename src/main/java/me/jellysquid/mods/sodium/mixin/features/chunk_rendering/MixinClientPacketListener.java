package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import me.jellysquid.mods.sodium.render.SodiumWorldRenderer;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {
    @Inject(method = "handleLevelChunkWithLight", at = @At("RETURN"))
    private void postLoadChunk(ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci) {
        SodiumWorldRenderer.instance()
                .onChunkAdded(packet.getX(), packet.getZ());
    }

    @Inject(method = "handleForgetLevelChunk", at = @At("RETURN"))
    private void postUnloadChunk(ClientboundForgetLevelChunkPacket packet, CallbackInfo ci) {
        SodiumWorldRenderer.instance()
                .onChunkRemoved(packet.getX(), packet.getZ());
    }
}
