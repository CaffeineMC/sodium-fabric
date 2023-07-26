package me.jellysquid.mods.sodium.client.render.chunk.data;

import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;

import java.util.Map;

public class ChunkMeshData {
    private final Map<ModelQuadFacing, VertexRange> parts;
    private final NativeBuffer buffer;

    private final int vertexCount;

    public ChunkMeshData(NativeBuffer buffer, Map<ModelQuadFacing, VertexRange> parts, int vertexCount) {
        this.parts = parts;
        this.buffer = buffer;

        this.vertexCount = vertexCount;
    }

    public NativeBuffer getVertexData() {
        return this.buffer;
    }

    public int getVertexCount() {
        return this.vertexCount;
    }

    public VertexRange getPart(ModelQuadFacing facing) {
        return this.parts.get(facing);
    }
}
