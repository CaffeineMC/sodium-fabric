package net.caffeinemc.sodium.render.chunk.state;

import net.caffeinemc.sodium.render.buffer.arena.BufferSegment;

public final class ChunkPassModel {
    private final long[] modelPartSegments;
    private final int visibilityBits;

    public ChunkPassModel(long[] modelPartSegments) {
        this.modelPartSegments = modelPartSegments;

        this.visibilityBits = calculateVisibilityBits(modelPartSegments);
    }

    public long[] getModelPartSegments() {
        return this.modelPartSegments;
    }

    public int getVisibilityBits() {
        return this.visibilityBits;
    }

    private static int calculateVisibilityBits(long[] modelPartSegments) {
        int flags = 0;

        for (int i = 0; i < modelPartSegments.length; i++) {
            if (modelPartSegments[i] != BufferSegment.INVALID) {
                flags |= 1 << i;
            }
        }

        return flags;
    }
}
