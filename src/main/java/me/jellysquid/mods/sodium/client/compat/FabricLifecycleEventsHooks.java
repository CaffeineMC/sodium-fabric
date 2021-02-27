package me.jellysquid.mods.sodium.client.compat;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.chunk.WorldChunk;

public interface FabricLifecycleEventsHooks {
    void invokeOnClientChunkLoad(ClientWorld world, WorldChunk chunk);
    void invokeOnClientChunkUnload(ClientWorld world, WorldChunk chunk);
}
