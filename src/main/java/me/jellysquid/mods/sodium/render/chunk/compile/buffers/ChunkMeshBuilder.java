package me.jellysquid.mods.sodium.render.chunk.compile.buffers;

import me.jellysquid.mods.sodium.render.terrain.quad.properties.ChunkMeshFace;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import me.jellysquid.mods.sodium.render.terrain.format.TerrainVertexSink;

public interface ChunkMeshBuilder {
    TerrainVertexSink getVertexSink(ChunkMeshFace face);

    void addSprite(TextureAtlasSprite sprite);

}
