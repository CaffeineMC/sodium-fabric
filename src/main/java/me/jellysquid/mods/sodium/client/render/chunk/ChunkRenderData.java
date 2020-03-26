package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkMeshInfo;

public interface ChunkRenderData {
    void destroy();

    void uploadChunk(ChunkMeshInfo layers);

    void deleteMeshes();
}
