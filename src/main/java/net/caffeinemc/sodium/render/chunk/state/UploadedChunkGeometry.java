package net.caffeinemc.sodium.render.chunk.state;

import net.caffeinemc.sodium.render.buffer.arena.BufferSegment;

public final class UploadedChunkGeometry {
    public final BufferSegment segment;
    public final ChunkPassModel[] models;

    public UploadedChunkGeometry(BufferSegment segment, ChunkPassModel[] models) {
        this.segment = segment;
        this.models = models;
    }

    public void delete() {
        this.segment.delete();
    }
}
