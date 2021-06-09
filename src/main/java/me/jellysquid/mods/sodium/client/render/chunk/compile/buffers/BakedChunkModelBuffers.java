package me.jellysquid.mods.sodium.client.render.chunk.compile.buffers;

import me.jellysquid.mods.sodium.client.model.PrimitiveSink;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;

public class BakedChunkModelBuffers implements ChunkModelBuffers {
    private final PrimitiveSink<ModelVertexSink>[] builders;
    private final ChunkRenderData.Builder renderData;

    public BakedChunkModelBuffers(PrimitiveSink<ModelVertexSink>[] builders,
                                  ChunkRenderData.Builder renderData) {
        this.builders = builders;
        this.renderData = renderData;
    }

    @Override
    public PrimitiveSink<ModelVertexSink> getBuilder(ModelQuadFacing facing) {
        return this.builders[facing.ordinal()];
    }

    @Override
    public ChunkRenderData.Builder getRenderData() {
        return this.renderData;
    }
}
