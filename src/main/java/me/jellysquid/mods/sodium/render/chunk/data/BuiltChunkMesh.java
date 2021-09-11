package me.jellysquid.mods.sodium.render.chunk.data;

import me.jellysquid.mods.sodium.render.IndexedMesh;
import me.jellysquid.mods.thingl.util.ElementRange;
import me.jellysquid.mods.sodium.model.quad.properties.ModelQuadFacing;

import java.util.Map;

public class BuiltChunkMesh {
    private final Map<ModelQuadFacing, ElementRange> faces;
    private final IndexedMesh vertexData;

    public BuiltChunkMesh(IndexedMesh vertexData, Map<ModelQuadFacing, ElementRange> faces) {
        this.faces = faces;
        this.vertexData = vertexData;
    }

    public Map<ModelQuadFacing, ElementRange> getFaces() {
        return this.faces;
    }

    public IndexedMesh getVertexData() {
        return this.vertexData;
    }
}
