package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.light.LightingProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {
    @Inject(method = "onChunkData", at = @At("RETURN"))
    private void postLoadChunk(ChunkDataS2CPacket packet, CallbackInfo ci) {
        SodiumWorldRenderer.instance()
                .onChunkAdded(packet.getX(), packet.getZ());
    }

    @Redirect(method = "updateLighting", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/light/LightingProvider;enqueueSectionData(Lnet/minecraft/world/LightType;Lnet/minecraft/util/math/ChunkSectionPos;Lnet/minecraft/world/chunk/ChunkNibbleArray;)V"))
    private void postLightUpdate(LightingProvider instance, LightType lightType, ChunkSectionPos pos, ChunkNibbleArray nibbles) {
        instance.enqueueSectionData(lightType, pos, nibbles);
        if (nibbles != null) {
            SodiumWorldRenderer.instance()
                    .onChunkLightAdded(pos.getX(), pos.getZ());
        }
    }

    @Inject(method = "onUnloadChunk", at = @At("RETURN"))
    private void postUnloadChunk(UnloadChunkS2CPacket packet, CallbackInfo ci) {
        SodiumWorldRenderer.instance()
                .onChunkRemoved(packet.getX(), packet.getZ());
    }
}
