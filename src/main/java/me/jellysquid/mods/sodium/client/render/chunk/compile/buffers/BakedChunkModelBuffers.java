package me.jellysquid.mods.sodium.client.render.chunk.compile.buffers;

import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;

public class BakedChunkModelBuffers implements ChunkModelBuffers {
    private final ModelVertexSink[] builders;

    public BakedChunkModelBuffers(ModelVertexSink[] builders) {
        this.builders = builders;
    }

    @Override
    public ModelVertexSink getSink(ModelQuadFacing facing) {
        return this.builders[facing.ordinal()];
    }
}
