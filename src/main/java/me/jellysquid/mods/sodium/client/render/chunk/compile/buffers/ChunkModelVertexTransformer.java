package me.jellysquid.mods.sodium.client.render.chunk.compile.buffers;

import me.jellysquid.mods.sodium.client.model.vertex.transformers.AbstractVertexTransformer;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkModelOffset;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;

public class ChunkModelVertexTransformer extends AbstractVertexTransformer<ModelVertexSink> implements ModelVertexSink {
    /**
     * The scale to be applied to all offsets and quads written into this mesh builder.
     */
    private static final float SCALE_NORM = 1.0f / 32.0f;

    /**
     * The translation to be applied to all quads written into this mesh builder.
     */
    private final ChunkModelOffset offset;

    public ChunkModelVertexTransformer(ModelVertexSink delegate, ChunkModelOffset offset) {
        super(delegate);

        this.offset = offset;
    }

    @Override
    public void writeQuad(float x, float y, float z, int color, float u, float v, int light) {
        x = (x * SCALE_NORM) + (this.offset.x * SCALE_NORM);
        y = (y * SCALE_NORM) + (this.offset.y * SCALE_NORM);
        z = (z * SCALE_NORM) + (this.offset.z * SCALE_NORM);

        this.delegate.writeQuad(x, y, z, color, u, v, light);
    }
}
