package me.jellysquid.mods.sodium.client.render.backends;

import me.jellysquid.mods.sodium.client.render.chunk.ChunkMesh;

import java.util.Collection;

public interface ChunkRenderState {
    void deleteData();

    void uploadData(Collection<ChunkMesh> meshes);
}
