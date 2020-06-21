package me.jellysquid.mods.sodium.client.render.backends.shader.cr;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.memory.BufferSegment;
import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderState;

public class CRRenderState implements ChunkRenderState {
    @Deprecated
    private final CRChunkRegion region;
    @Deprecated
    private final BufferSegment segment;
    private final int start, length;

    public CRRenderState(CRChunkRegion region, BufferSegment segment, GlVertexFormat<?> format) {
        this.region = region;
        this.segment = segment;
        this.start = segment.getStart() / format.getStride();
        this.length = segment.getLength() / format.getStride();
    }

    @Override
    public void delete() {
        this.segment.delete();
    }

    @Deprecated
    public BufferSegment getSegment() {
        return this.segment;
    }

    @Deprecated
    public CRChunkRegion getRegion() {
        return this.region;
    }

    public int getStart() {
        return this.start;
    }

    public int getLength() {
        return this.length;
    }
}
