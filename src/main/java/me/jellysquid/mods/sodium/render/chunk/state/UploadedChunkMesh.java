package me.jellysquid.mods.sodium.render.chunk.state;

import me.jellysquid.mods.sodium.render.arena.GlBufferSegment;
import me.jellysquid.mods.sodium.render.buffer.ElementRange;
import me.jellysquid.mods.sodium.render.terrain.quad.properties.ChunkMeshFace;
import org.apache.commons.lang3.Validate;

import java.util.Map;

public class UploadedChunkMesh {
    private final GlBufferSegment vertexSegment;
    private final GlBufferSegment indexSegment;

    private final ElementRange[] parts;
    private final int visibilityFlags;

    public UploadedChunkMesh(GlBufferSegment vertexSegment, GlBufferSegment indexSegment, ChunkMesh data) {
        Validate.notNull(vertexSegment);
        Validate.notNull(indexSegment);

        this.vertexSegment = vertexSegment;
        this.indexSegment = indexSegment;

        this.parts = new ElementRange[ChunkMeshFace.COUNT];

        for (Map.Entry<ChunkMeshFace, ElementRange> entry : data.getParts().entrySet()) {
            this.parts[entry.getKey().ordinal()] = entry.getValue();
        }

        this.visibilityFlags = calculateVisibilityFlags(this.parts);
    }

    private static int calculateVisibilityFlags(ElementRange[] parts) {
        int flags = 0;

        for (int i = 0; i < parts.length; i++) {
            if (parts[i] != null) {
                flags |= 1 << i;
            }
        }

        return flags;
    }

    public void delete() {
        this.vertexSegment.delete();
        this.indexSegment.delete();
    }

    public ElementRange getMeshPart(int face) {
        return this.parts[face];
    }

    public GlBufferSegment getVertexSegment() {
        return this.vertexSegment;
    }

    public GlBufferSegment getIndexSegment() {
        return this.indexSegment;
    }

    public int getVisibilityFlags() {
        return this.visibilityFlags;
    }
}
