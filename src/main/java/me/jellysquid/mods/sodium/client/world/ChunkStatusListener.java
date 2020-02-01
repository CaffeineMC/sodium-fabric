package me.jellysquid.mods.sodium.client.world;

public interface ChunkStatusListener {
    void onChunkAdded(int x, int z);

    void onChunkRemoved(int x, int z);
}
