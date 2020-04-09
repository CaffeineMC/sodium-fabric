package me.jellysquid.mods.sodium.client.render.backends;

import me.jellysquid.mods.sodium.client.render.chunk.ChunkLayerInfo;

import java.util.Collection;

public interface ChunkRenderState {
    void clearData();

    void uploadData(Collection<ChunkLayerInfo> layers);
}
