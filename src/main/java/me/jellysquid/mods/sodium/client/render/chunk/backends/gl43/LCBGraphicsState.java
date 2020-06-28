package me.jellysquid.mods.sodium.client.render.chunk.backends.gl43;

import me.jellysquid.mods.sodium.client.gl.arena.GlBufferRegion;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.util.BufferSlice;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.region.ChunkRegion;

import java.util.Map;

public class LCBGraphicsState implements ChunkGraphicsState {
    private final ChunkRegion<LCBGraphicsState> region;

    private final GlBufferRegion segment;
    private final long[] parts;
    private int facesWithData;

    public LCBGraphicsState(ChunkRegion<LCBGraphicsState> region, GlBufferRegion segment, ChunkMeshData meshData, GlVertexFormat<?> vertexFormat) {
        this.region = region;
        this.segment = segment;

        this.parts = new long[ModelQuadFacing.COUNT];

        for (Map.Entry<ModelQuadFacing, BufferSlice> entry : meshData.getSlices()) {
            ModelQuadFacing facing = entry.getKey();
            BufferSlice slice = entry.getValue();

            int start = (segment.getStart() + slice.start) / vertexFormat.getStride();
            int count = slice.len / vertexFormat.getStride();

            this.parts[facing.ordinal()] = BufferSlice.pack(start, count);
            this.facesWithData |= 1 << facing.ordinal();
        }
    }

    @Override
    public void delete() {
        this.segment.delete();
    }

    public ChunkRegion<LCBGraphicsState> getRegion() {
        return this.region;
    }

    public long getModelPart(int facing) {
        return this.parts[facing];
    }

    public int getFacesWithData() {
        return this.facesWithData;
    }
}
