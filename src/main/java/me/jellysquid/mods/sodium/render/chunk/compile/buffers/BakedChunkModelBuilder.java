package me.jellysquid.mods.sodium.render.chunk.compile.buffers;

import me.jellysquid.mods.sodium.model.IndexBufferBuilder;
import me.jellysquid.mods.sodium.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.render.chunk.format.ModelVertexSink;
import net.minecraft.client.texture.Sprite;

public class BakedChunkModelBuilder implements ChunkModelBuilder {
    private final ModelVertexSink vertexSink;
    private final IndexBufferBuilder[] indexBufferBuilders;

    private final ChunkRenderData.Builder renderData;

    public BakedChunkModelBuilder(IndexBufferBuilder[] indexBufferBuilders,
                                  ModelVertexSink vertexSink,
                                  ChunkRenderData.Builder renderData) {
        this.indexBufferBuilders = indexBufferBuilders;
        this.vertexSink = vertexSink;

        this.renderData = renderData;
    }

    @Override
    public ModelVertexSink getVertexSink() {
        return this.vertexSink;
    }

    @Override
    public IndexBufferBuilder getIndexBufferBuilder(ModelQuadFacing facing) {
        return this.indexBufferBuilders[facing.ordinal()];
    }

    @Override
    public void addSprite(Sprite sprite) {
        this.renderData.addSprite(sprite);
    }
}
