package net.caffeinemc.mods.sodium.client.render.chunk.map;

public interface ClientChunkEventListener {
    void updateMapCenter(int chunkX, int chunkZ);

    void updateLoadDistance(int loadDistance);

    void onChunkStatusAdded(int x, int z, int flags);
    void onChunkStatusRemoved(int x, int z, int flags);
}
