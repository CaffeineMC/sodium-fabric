package me.jellysquid.mods.sodium.client.render.chunk.data;

import me.jellysquid.mods.sodium.client.gl.buffer.IndexedVertexData;
import me.jellysquid.mods.sodium.client.gl.util.ElementRange;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;

import java.util.EnumMap;
import java.util.Map;

public class ChunkMeshData {
    public static final ChunkMeshData EMPTY = new ChunkMeshData();

    private final EnumMap<ModelQuadFacing, ElementRange> parts = new EnumMap<>(ModelQuadFacing.class);
    private IndexedVertexData vertexData;

    public void setVertexData(IndexedVertexData vertexData) {
        this.vertexData = vertexData;
    }

    public void setModelSlice(ModelQuadFacing facing, ElementRange slice) {
        this.parts.put(facing, slice);
    }

    public IndexedVertexData takeVertexData() {
        IndexedVertexData data = this.vertexData;

        if (data == null) {
            throw new NullPointerException("No pending data to upload");
        }

        this.vertexData = null;

        return data;
    }

    public boolean hasVertexData() {
        return this.vertexData != null;
    }

    public int getVertexDataSize() {
        if (this.vertexData != null) {
            return this.vertexData.vertexBuffer.capacity();
        }

        return 0;
    }

    public Iterable<? extends Map.Entry<ModelQuadFacing, ElementRange>> getSlices() {
        return this.parts.entrySet();
    }
}
