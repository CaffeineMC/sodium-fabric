package me.jellysquid.mods.sodium.client.render.chunk.backends.gl46;

import it.unimi.dsi.fastutil.bytes.Byte2ReferenceMap;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferRegion;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkModelPart;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkModelSlice;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.region.ChunkRegion;

public class LCBGraphicsState implements ChunkGraphicsState {
    private final ChunkRegion<LCBGraphicsState> region;

    private final GlBufferRegion segment;
    private final ChunkModelPart[] parts;

    public LCBGraphicsState(ChunkRegion<LCBGraphicsState> region, GlBufferRegion segment, ChunkMeshData meshData, GlVertexFormat<?> vertexFormat) {
        this.region = region;
        this.segment = segment;

        this.parts = new ChunkModelPart[ChunkModelPart.count()];

        for (Byte2ReferenceMap.Entry<ChunkModelSlice> entry : meshData.getBuffers().byte2ReferenceEntrySet()) {
            ChunkModelSlice slice = entry.getValue();

            int start = (segment.getStart() + slice.start) / vertexFormat.getStride();
            int count = slice.len / vertexFormat.getStride();

            this.parts[entry.getByteKey()] = new ChunkModelPart(start, count);
        }
    }

    @Override
    public void delete() {
        this.segment.delete();
    }

    public ChunkRegion<LCBGraphicsState> getRegion() {
        return this.region;
    }

    public ChunkModelPart getModelPart(byte key) {
        return this.parts[key];
    }
}
