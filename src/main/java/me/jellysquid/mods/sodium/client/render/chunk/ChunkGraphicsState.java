package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.gl.arena.GlBufferSegment;
import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import org.apache.commons.lang3.Validate;

import java.util.Map;

public class ChunkGraphicsState {
    private final GlBufferSegment vertexSegment;

    private final VertexRange[] parts;

    private final int largestPrimitiveBatchSize;

    public ChunkGraphicsState(GlBufferSegment vertexSegment, ChunkMeshData data) {
        Validate.notNull(vertexSegment);

        this.vertexSegment = vertexSegment;

        this.parts = new VertexRange[ModelQuadFacing.COUNT];

        int largestPrimitiveBatchSize = 0;

        for (Map.Entry<ModelQuadFacing, VertexRange> entry : data.getParts().entrySet()) {
            this.parts[entry.getKey().ordinal()] = entry.getValue();
            largestPrimitiveBatchSize = Math.max(largestPrimitiveBatchSize, vertexSegment.getLength() / 4);
        }

        this.largestPrimitiveBatchSize = largestPrimitiveBatchSize;
    }

    public void delete() {
        this.vertexSegment.delete();
    }

    public VertexRange getModelPart(ModelQuadFacing facing) {
        return this.parts[facing.ordinal()];
    }

    public GlBufferSegment getVertexSegment() {
        return this.vertexSegment;
    }

    public int getLargestPrimitiveBatchSize() {
        return this.largestPrimitiveBatchSize;
    }
}
