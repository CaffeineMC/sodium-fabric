package me.jellysquid.mods.sodium.client.world;

import net.minecraft.util.math.ChunkPos;

public interface ChunkLightUnloadQueue {
    void enqueueUnload(ChunkPos columnPos);
}
