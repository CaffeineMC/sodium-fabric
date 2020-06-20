package me.jellysquid.mods.sodium.mixin.light_unloading;

import me.jellysquid.mods.sodium.client.world.ChunkLightUnloadQueue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {
    @Shadow
    private MinecraftClient client;

    @Shadow
    private ClientWorld world;

    /**
     * @reason replace with implementation the enqueues unload lighting updates to happen over multiple ticks
     * @author gegy1000
     */
    @Overwrite
    public void onUnloadChunk(UnloadChunkS2CPacket packet) {
        ClientPlayNetworkHandler self = (ClientPlayNetworkHandler) (Object) this;
        NetworkThreadUtils.forceMainThread(packet, self, this.client);

        int columnX = packet.getX();
        int columnZ = packet.getZ();

        ClientChunkManager chunkManager = this.world.getChunkManager();
        chunkManager.unload(columnX, columnZ);

        ((ChunkLightUnloadQueue) chunkManager).enqueueUnload(new ChunkPos(columnX, columnZ));
    }
}
