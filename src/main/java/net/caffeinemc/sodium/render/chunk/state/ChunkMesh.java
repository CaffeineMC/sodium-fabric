package net.caffeinemc.sodium.render.chunk.state;

import net.caffeinemc.sodium.render.buffer.IndexedVertexData;
import net.caffeinemc.sodium.render.buffer.VertexRange;
import net.caffeinemc.sodium.render.terrain.quad.properties.ChunkMeshFace;

import java.util.Map;

public class ChunkMesh {
    private final Map<ChunkMeshFace, VertexRange> parts;
    private final IndexedVertexData vertexData;

    public ChunkMesh(IndexedVertexData vertexData, Map<ChunkMeshFace, VertexRange> parts) {
        this.parts = parts;
        this.vertexData = vertexData;
    }

    public Map<ChunkMeshFace, VertexRange> getParts() {
        return this.parts;
    }

    public IndexedVertexData getVertexData() {
        return this.vertexData;
    }
}
