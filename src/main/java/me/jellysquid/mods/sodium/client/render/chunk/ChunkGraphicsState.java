package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.gl.arena.GlBufferSegment;
import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import org.apache.commons.lang3.Validate;

import java.util.Map;

public class ChunkGraphicsState {
    private final GlBufferSegment vertexSegment;

    private final VertexRange model;
    private final VertexRange[] parts;

    private final int flags;

    public ChunkGraphicsState(GlBufferSegment vertexSegment, ChunkMeshData data) {
        Validate.notNull(vertexSegment);

        this.vertexSegment = vertexSegment;

        this.model = new VertexRange(0, data.getVertexCount());
        this.parts = new VertexRange[ModelQuadFacing.COUNT];

        int flags = 0;

        for (Map.Entry<ModelQuadFacing, VertexRange> entry : data.getParts().entrySet()) {
            this.parts[entry.getKey().ordinal()] = entry.getValue();
            flags |= 1 << entry.getKey().ordinal();
        }

        this.flags = flags;
    }

    public void delete() {
        this.vertexSegment.delete();
    }

    public VertexRange getModelPart(int facing) {
        return this.parts[facing];
    }

    public GlBufferSegment getVertexSegment() {
        return this.vertexSegment;
    }

    public int getFlags() {
        return this.flags;
    }

    public VertexRange getModel() {
        return this.model;
    }
}
