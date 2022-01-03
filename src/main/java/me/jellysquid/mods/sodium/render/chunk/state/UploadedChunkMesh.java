package me.jellysquid.mods.sodium.render.chunk.state;

import me.jellysquid.mods.sodium.render.arena.GlBufferSegment;
import me.jellysquid.mods.sodium.render.buffer.ElementRange;
import me.jellysquid.mods.sodium.render.terrain.quad.properties.ChunkMeshFace;
import org.apache.commons.lang3.Validate;

import java.util.Map;

@Deprecated
public class UploadedChunkMesh {
    private final GlBufferSegment vertexSegment;
    private final GlBufferSegment indexSegment;

    private final ElementRange[] parts;

    public UploadedChunkMesh(GlBufferSegment vertexSegment, GlBufferSegment indexSegment, ChunkMesh data) {
        Validate.notNull(vertexSegment);
        Validate.notNull(indexSegment);

        this.vertexSegment = vertexSegment;
        this.indexSegment = indexSegment;

        this.parts = new ElementRange[ChunkMeshFace.COUNT];

        for (Map.Entry<ChunkMeshFace, ElementRange> entry : data.getParts().entrySet()) {
            this.parts[entry.getKey().ordinal()] = entry.getValue();
        }
    }

    public void delete() {
        this.vertexSegment.delete();
        this.indexSegment.delete();
    }

    public ElementRange getMeshPart(ChunkMeshFace facing) {
        return this.parts[facing.ordinal()];
    }

    public GlBufferSegment getVertexSegment() {
        return this.vertexSegment;
    }

    public GlBufferSegment getIndexSegment() {
        return this.indexSegment;
    }
}
