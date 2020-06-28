package me.jellysquid.mods.sodium.client.render.chunk.oneshot;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.util.BufferSlice;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;

import java.util.Arrays;
import java.util.Map;

public abstract class ChunkOneshotGraphicsState extends ChunkGraphicsState {
    private final long[] parts;
    private int facesWithData;

    protected ChunkOneshotGraphicsState(ChunkRenderContainer<?> container) {
        super(container);

        this.parts = new long[ModelQuadFacing.COUNT];
    }

    public long getModelPart(int facing) {
        return this.parts[facing];
    }

    protected void setModelPart(ModelQuadFacing facing, long slice) {
        this.parts[facing.ordinal()] = slice;
        this.facesWithData |= 1 << facing.ordinal();
    }

    public abstract void upload(ChunkMeshData mesh);

    public abstract void bind();

    protected void setupModelParts(ChunkMeshData meshData, GlVertexFormat<?> vertexFormat) {
        int stride = vertexFormat.getStride();

        Arrays.fill(this.parts, 0L);
        this.facesWithData = 0;

        for (Map.Entry<ModelQuadFacing, BufferSlice> entry : meshData.getSlices()) {
            ModelQuadFacing facing = entry.getKey();
            BufferSlice slice = entry.getValue();

            this.setModelPart(facing, BufferSlice.pack(slice.start / stride, slice.len / stride));
        }
    }
}
