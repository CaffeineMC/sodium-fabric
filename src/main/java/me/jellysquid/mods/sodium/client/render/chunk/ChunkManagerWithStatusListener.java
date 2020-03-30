package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.world.ChunkStatusListener;

public interface ChunkManagerWithStatusListener {
    void setListener(ChunkStatusListener listener);
}
