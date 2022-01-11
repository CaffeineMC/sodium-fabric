package me.jellysquid.mods.sodium.render.chunk.compile.buffers;

import me.jellysquid.mods.sodium.render.chunk.state.ChunkRenderData;
import me.jellysquid.mods.sodium.render.terrain.format.TerrainVertexSink;
import me.jellysquid.mods.sodium.render.terrain.quad.properties.ChunkMeshFace;
import net.minecraft.client.texture.Sprite;

public class DefaultChunkMeshBuilder implements ChunkMeshBuilder {
    private final TerrainVertexSink[] sinks;
    private final ChunkRenderData.Builder renderData;

    public DefaultChunkMeshBuilder(TerrainVertexSink[] sinks,
                                   ChunkRenderData.Builder renderData) {
        this.sinks = sinks;
        this.renderData = renderData;
    }

    @Override
    public TerrainVertexSink getVertexSink(ChunkMeshFace face) {
        return this.sinks[face.ordinal()];
    }

    @Override
    public void addSprite(Sprite sprite) {
        this.renderData.addSprite(sprite);
    }

}
