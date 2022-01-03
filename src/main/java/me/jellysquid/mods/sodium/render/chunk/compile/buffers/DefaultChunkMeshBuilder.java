package me.jellysquid.mods.sodium.render.chunk.compile.buffers;

import me.jellysquid.mods.sodium.render.terrain.quad.properties.ChunkMeshFace;
import me.jellysquid.mods.sodium.render.chunk.state.ChunkRenderData;
import me.jellysquid.mods.sodium.render.terrain.format.TerrainVertexSink;
import net.minecraft.client.texture.Sprite;

public class DefaultChunkMeshBuilder implements ChunkMeshBuilder {
    private final TerrainVertexSink vertexSink;
    private final IndexBufferBuilder[] indexBufferBuilders;

    private final ChunkRenderData.Builder renderData;
    private final int id;

    public DefaultChunkMeshBuilder(IndexBufferBuilder[] indexBufferBuilders,
                                   TerrainVertexSink vertexSink,
                                   ChunkRenderData.Builder renderData,
                                   int chunkId) {
        this.indexBufferBuilders = indexBufferBuilders;
        this.vertexSink = vertexSink;

        this.renderData = renderData;
        this.id = chunkId;
    }

    @Override
    public TerrainVertexSink getVertexSink() {
        return this.vertexSink;
    }

    @Override
    public IndexBufferBuilder getIndexBufferBuilder(ChunkMeshFace facing) {
        return this.indexBufferBuilders[facing.ordinal()];
    }

    @Override
    public void addSprite(Sprite sprite) {
        this.renderData.addSprite(sprite);
    }

    @Override
    public int getChunkId() {
        return this.id;
    }
}
