package me.jellysquid.mods.sodium.client.render.chunk.compile.buffers;

import me.jellysquid.mods.sodium.client.model.IndexBufferBuilder;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import net.minecraft.client.texture.Sprite;

public class BakedChunkModelBuilder implements ChunkModelBuilder {
    private final ChunkMeshBufferBuilder vertexBuffer;
    private final IndexBufferBuilder[] indexBuffers;

    private final ChunkRenderData.Builder renderData;

    public BakedChunkModelBuilder(ChunkMeshBufferBuilder vertexBuffer, IndexBufferBuilder[] indexBuffers,
                                  ChunkRenderData.Builder renderData) {
        this.indexBuffers = indexBuffers;
        this.vertexBuffer = vertexBuffer;

        this.renderData = renderData;
    }

    @Override
    public ChunkMeshBufferBuilder getVertexBuffer() {
        return this.vertexBuffer;
    }

    @Override
    public IndexBufferBuilder getIndexBuffer(ModelQuadFacing facing) {
        return this.indexBuffers[facing.ordinal()];
    }

    @Override
    public void addSprite(Sprite sprite) {
        this.renderData.addSprite(sprite);
    }

}
