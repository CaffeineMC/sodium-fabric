package me.jellysquid.mods.sodium.client.render.chunk.backends.multidraw;

import me.jellysquid.mods.sodium.client.gl.arena.GlBufferSegment;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.util.BufferSlice;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.region.ChunkRegion;

import java.util.Map;

public class MultidrawGraphicsState extends ChunkGraphicsState {
    private final ChunkRegion<MultidrawGraphicsState> region;

    private final GlBufferSegment segment;
    private final long[] parts;

    public MultidrawGraphicsState(ChunkRenderContainer<?> container, ChunkRegion<MultidrawGraphicsState> region, GlBufferSegment segment, ChunkMeshData meshData, GlVertexFormat<?> vertexFormat) {
        super(container);

        this.region = region;
        this.segment = segment;

        this.parts = new long[ModelQuadFacing.COUNT];

        for (Map.Entry<ModelQuadFacing, BufferSlice> entry : meshData.getSlices()) {
            ModelQuadFacing facing = entry.getKey();
            BufferSlice slice = entry.getValue();

            int start = (segment.getStart() + slice.start) / vertexFormat.getStride();
            int count = slice.len / vertexFormat.getStride();

            this.parts[facing.ordinal()] = BufferSlice.pack(start, count);
        }
    }

    @Override
    public void delete(CommandList commandList) {
        this.segment.delete();
    }

    public ChunkRegion<MultidrawGraphicsState> getRegion() {
        return this.region;
    }

    public long getModelPart(int facing) {
        return this.parts[facing];
    }

}
