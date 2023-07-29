package me.jellysquid.mods.sodium.client.render.chunk.map;

import net.minecraft.client.world.ClientWorld;

public interface ChunkTrackerHolder {
    static ChunkTracker get(ClientWorld world) {
        return ((ChunkTrackerHolder) world).sodium$getTracker();
    }

    ChunkTracker sodium$getTracker();
}
