package me.jellysquid.mods.sodium.client.render.chunk.data;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.buffer.VertexData;
import me.jellysquid.mods.sodium.client.gl.util.BufferSlice;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;

import java.util.Map;

public class ChunkMeshData {
    private final Map<ModelQuadFacing, BufferSlice> parts = new Object2ObjectLinkedOpenHashMap<>();
    private VertexData vertexData;

    public void setVertexData(VertexData vertexData) {
        this.vertexData = vertexData;
    }

    public void setModelSlice(ModelQuadFacing facing, BufferSlice slice) {
        this.parts.put(facing, slice);
    }

    public VertexData takeVertexData() {
        VertexData data = this.vertexData;

        if (data == null) {
            throw new NullPointerException("No pending data to upload");
        }

        this.vertexData = null;

        return data;
    }

    public int getVertexDataSize() {
        if (this.vertexData != null) {
            return this.vertexData.buffer.capacity();
        }

        return 0;
    }

    public Iterable<? extends Map.Entry<ModelQuadFacing, BufferSlice>> getSlices() {
        return this.parts.entrySet();
    }
}
