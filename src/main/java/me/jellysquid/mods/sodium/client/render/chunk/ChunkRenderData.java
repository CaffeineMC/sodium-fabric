package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkMeshInfo;

public interface ChunkRenderData {
    void clearData();

    void uploadData(ChunkMeshInfo layers);
}
