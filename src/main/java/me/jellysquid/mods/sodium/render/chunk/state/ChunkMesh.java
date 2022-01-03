package me.jellysquid.mods.sodium.render.chunk.state;

import me.jellysquid.mods.sodium.render.chunk.buffer.IndexedVertexData;
import me.jellysquid.mods.sodium.render.chunk.buffer.ElementRange;
import me.jellysquid.mods.sodium.render.terrain.quad.properties.ChunkMeshFace;

import java.util.Map;

public class ChunkMesh {
    private final Map<ChunkMeshFace, ElementRange> parts;
    private final IndexedVertexData vertexData;

    public ChunkMesh(IndexedVertexData vertexData, Map<ChunkMeshFace, ElementRange> parts) {
        this.parts = parts;
        this.vertexData = vertexData;
    }

    public Map<ChunkMeshFace, ElementRange> getParts() {
        return this.parts;
    }

    public IndexedVertexData getVertexData() {
        return this.vertexData;
    }
}
