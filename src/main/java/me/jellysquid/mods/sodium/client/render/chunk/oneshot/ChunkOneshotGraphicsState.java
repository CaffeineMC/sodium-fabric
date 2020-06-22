package me.jellysquid.mods.sodium.client.render.chunk.oneshot;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.util.BufferSlice;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;

import java.util.Arrays;
import java.util.Map;

public abstract class ChunkOneshotGraphicsState implements ChunkGraphicsState {
    private final BufferSlice[] parts;

    protected ChunkOneshotGraphicsState() {
        this.parts = new BufferSlice[ModelQuadFacing.COUNT];
    }

    public BufferSlice getModelPart(ModelQuadFacing facing) {
        return this.parts[facing.ordinal()];
    }

    protected void setModelPart(ModelQuadFacing facing, BufferSlice part) {
        this.parts[facing.ordinal()] = part;
    }

    public abstract void upload(ChunkMeshData mesh);

    public abstract void bind();

    protected void setupModelParts(ChunkMeshData meshData, GlVertexFormat<?> vertexFormat) {
        int stride = vertexFormat.getStride();

        Arrays.fill(this.parts, null);

        for (Map.Entry<ModelQuadFacing, me.jellysquid.mods.sodium.client.gl.util.BufferSlice> entry : meshData.getSlices()) {
            ModelQuadFacing facing = entry.getKey();
            me.jellysquid.mods.sodium.client.gl.util.BufferSlice slice = entry.getValue();

            this.setModelPart(facing, new BufferSlice(slice.start / stride, slice.len / stride));
        }
    }
}
