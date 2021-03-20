package me.jellysquid.mods.sodium.client.render.chunk.oneshot;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.util.BufferSlice;
import me.jellysquid.mods.sodium.client.gl.util.MemoryTracker;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;

import java.util.Arrays;
import java.util.Map;

public abstract class ChunkOneshotGraphicsState extends ChunkGraphicsState {
    protected final MemoryTracker memoryTracker;
    private final long[] parts;

    protected ChunkOneshotGraphicsState(MemoryTracker memoryTracker, ChunkRenderContainer container, int id) {
        super(container, id);

        this.memoryTracker = memoryTracker;
        this.parts = new long[ModelQuadFacing.COUNT];
    }

    public long getModelPart(int facing) {
        return this.parts[facing];
    }

    protected void setModelPart(ModelQuadFacing facing, long slice) {
        this.parts[facing.ordinal()] = slice;
    }

    public abstract void upload(ChunkMeshData mesh);

    public abstract void bind();

    protected void setupModelParts(ChunkMeshData meshData, GlVertexFormat<?> vertexFormat) {
        int stride = vertexFormat.getStride();

        Arrays.fill(this.parts, 0L);

        for (Map.Entry<ModelQuadFacing, BufferSlice> entry : meshData.getSlices()) {
            ModelQuadFacing facing = entry.getKey();
            BufferSlice slice = entry.getValue();

            this.setModelPart(facing, BufferSlice.pack(slice.start / stride, slice.len / stride));
        }
    }
}
