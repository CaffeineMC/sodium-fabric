package me.jellysquid.mods.sodium.client.render.chunk.data;

import me.jellysquid.mods.sodium.client.render.IndexedVertexData;
import me.jellysquid.mods.thingl.util.ElementRange;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;

import java.util.Map;

public class ChunkMeshData {
    private final Map<ModelQuadFacing, ElementRange> parts;
    private final IndexedVertexData vertexData;

    public ChunkMeshData(IndexedVertexData vertexData, Map<ModelQuadFacing, ElementRange> parts) {
        this.parts = parts;
        this.vertexData = vertexData;
    }

    public Map<ModelQuadFacing, ElementRange> getParts() {
        return this.parts;
    }

    public IndexedVertexData getVertexData() {
        return this.vertexData;
    }
}
