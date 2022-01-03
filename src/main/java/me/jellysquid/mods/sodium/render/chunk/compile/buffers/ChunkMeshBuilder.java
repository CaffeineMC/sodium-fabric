package me.jellysquid.mods.sodium.render.chunk.compile.buffers;

import me.jellysquid.mods.sodium.render.terrain.quad.properties.ChunkMeshFace;
import me.jellysquid.mods.sodium.render.terrain.format.TerrainVertexSink;
import net.minecraft.client.texture.Sprite;

public interface ChunkMeshBuilder {
    TerrainVertexSink getVertexSink();

    IndexBufferBuilder getIndexBufferBuilder(ChunkMeshFace facing);

    void addSprite(Sprite sprite);

    int getChunkId();
}
