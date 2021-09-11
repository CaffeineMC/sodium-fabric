package me.jellysquid.mods.sodium.render.chunk.renderer;

import me.jellysquid.mods.sodium.render.chunk.arena.GlBufferSegment;
import me.jellysquid.mods.thingl.util.ElementRange;
import me.jellysquid.mods.sodium.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.render.chunk.data.BuiltChunkMesh;
import org.apache.commons.lang3.Validate;

import java.util.Map;

public class ChunkGraphicsState {
    private final GlBufferSegment vertexSegment;
    private final GlBufferSegment indexSegment;

    private final ElementRange[] faces;

    public ChunkGraphicsState(GlBufferSegment vertexSegment, GlBufferSegment indexSegment, BuiltChunkMesh data) {
        Validate.notNull(vertexSegment);
        Validate.notNull(indexSegment);

        this.vertexSegment = vertexSegment;
        this.indexSegment = indexSegment;

        this.faces = new ElementRange[ModelQuadFacing.COUNT];

        for (Map.Entry<ModelQuadFacing, ElementRange> entry : data.getFaces().entrySet()) {
            this.faces[entry.getKey().ordinal()] = entry.getValue();
        }
    }

    public void delete() {
        this.vertexSegment.delete();
        this.indexSegment.delete();
    }

    public ElementRange getModelFace(int face) {
        return this.faces[face];
    }

    public GlBufferSegment getVertexSegment() {
        return this.vertexSegment;
    }

    public GlBufferSegment getIndexSegment() {
        return this.indexSegment;
    }
}
