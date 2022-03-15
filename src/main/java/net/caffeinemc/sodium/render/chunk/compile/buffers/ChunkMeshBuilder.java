package net.caffeinemc.sodium.render.chunk.compile.buffers;

import net.caffeinemc.sodium.render.terrain.quad.properties.ChunkMeshFace;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexSink;
import net.minecraft.client.texture.Sprite;

public interface ChunkMeshBuilder {
    TerrainVertexSink getVertexSink(ChunkMeshFace face);

    void addSprite(Sprite sprite);

}
