package net.caffeinemc.mods.sodium.client.render.chunk.map;

import net.minecraft.client.multiplayer.ClientLevel;

public interface ChunkTrackerHolder {
    static ChunkTracker get(ClientLevel level) {
        return ((ChunkTrackerHolder) level).sodium$getTracker();
    }

    ChunkTracker sodium$getTracker();
}
