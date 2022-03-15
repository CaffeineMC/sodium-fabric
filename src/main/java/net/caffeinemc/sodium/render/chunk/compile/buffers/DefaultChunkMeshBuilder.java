package net.caffeinemc.sodium.render.chunk.compile.buffers;

import net.caffeinemc.sodium.render.chunk.state.ChunkRenderData;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexSink;
import net.caffeinemc.sodium.render.terrain.quad.properties.ChunkMeshFace;
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
