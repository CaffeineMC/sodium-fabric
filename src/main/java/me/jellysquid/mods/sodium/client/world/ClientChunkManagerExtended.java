package me.jellysquid.mods.sodium.client.world;

import it.unimi.dsi.fastutil.longs.LongCollection;

public interface ClientChunkManagerExtended {
    void afterLightChunk(int x, int z);

    // TODO: allow multiple listeners to be added?
    void setListener(ChunkStatusListener listener);

    LongCollection getLoadedChunks();
}
