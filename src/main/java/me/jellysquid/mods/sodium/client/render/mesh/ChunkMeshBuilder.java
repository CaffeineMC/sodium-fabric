package me.jellysquid.mods.sodium.client.render.mesh;

import me.jellysquid.mods.sodium.client.render.quad.ModelQuadView;

public interface ChunkMeshBuilder {
    void write(ModelQuadView quad);

    boolean isEmpty();
}
