package me.jellysquid.mods.sodium.client.render.chunk.backends.gl46;

import me.jellysquid.mods.sodium.client.gl.arena.GlBufferRegion;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.util.BufferSlice;
import me.jellysquid.mods.sodium.client.gl.util.VertexSlice;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.region.ChunkRegion;

public class LCBGraphicsState implements ChunkGraphicsState {
    private final ChunkRegion<LCBGraphicsState> region;

    private final GlBufferRegion segment;
    private final long[] layers;

    public LCBGraphicsState(ChunkRegion<LCBGraphicsState> region, GlBufferRegion segment, ChunkMeshData meshData, GlVertexFormat<?> vertexFormat) {
        this.region = region;
        this.segment = segment;

        this.layers = new long[BlockRenderPass.count()];

        for (BlockRenderPass pass : BlockRenderPass.VALUES) {
            BufferSlice slice = meshData.getSlice(pass);

            if (slice != null) {
                int start = (segment.getStart() + slice.start) / vertexFormat.getStride();
                int count = slice.len / vertexFormat.getStride();

                this.layers[pass.ordinal()] = VertexSlice.pack(start, count);
            }
        }
    }

    @Override
    public void delete() {
        this.segment.delete();
    }

    public ChunkRegion<LCBGraphicsState> getRegion() {
        return this.region;
    }

    public long getSliceForLayer(BlockRenderPass pass) {
        return this.layers[pass.ordinal()];
    }
}
