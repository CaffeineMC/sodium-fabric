package me.jellysquid.mods.sodium.render.chunk.state;

import me.jellysquid.mods.sodium.render.arena.GlBufferSegment;
import me.jellysquid.mods.sodium.render.buffer.ElementRange;
import me.jellysquid.mods.sodium.render.buffer.VertexRange;
import me.jellysquid.mods.sodium.render.terrain.quad.properties.ChunkMeshFace;
import org.apache.commons.lang3.Validate;

import java.util.Map;

public class UploadedChunkMesh {
    private final GlBufferSegment vertexSegment;

    private final VertexRange[] parts;
    private final int visibilityFlags;

    public UploadedChunkMesh(GlBufferSegment vertexSegment, ChunkMesh data) {
        Validate.notNull(vertexSegment);

        this.vertexSegment = vertexSegment;

        this.parts = new VertexRange[ChunkMeshFace.COUNT];

        for (Map.Entry<ChunkMeshFace, VertexRange> entry : data.getParts().entrySet()) {
            this.parts[entry.getKey().ordinal()] = entry.getValue();
        }

        this.visibilityFlags = calculateVisibilityFlags(this.parts);
    }

    private static int calculateVisibilityFlags(Object[] parts) {
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
    }

    public VertexRange getMeshPart(int face) {
        return this.parts[face];
    }

    public GlBufferSegment getVertexSegment() {
        return this.vertexSegment;
    }

    public int getVisibilityFlags() {
        return this.visibilityFlags;
    }
}
