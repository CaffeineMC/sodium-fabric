package net.caffeinemc.sodium.render.chunk.state;

import net.caffeinemc.sodium.render.buffer.VertexRange;

public final class ChunkPassModel {
    private final VertexRange[] ranges;
    private final int visibilityBits;

    public ChunkPassModel(VertexRange[] ranges) {
        this.ranges = ranges;

        this.visibilityBits = calculateVisibilityBits(ranges);
    }

    public VertexRange[] getModelParts() {
        return this.ranges;
    }

    public int getVisibilityBits() {
        return this.visibilityBits;
    }

    private static int calculateVisibilityBits(VertexRange[] parts) {
        int flags = 0;

        for (int i = 0; i < parts.length; i++) {
            if (parts[i] != null) {
                flags |= 1 << i;
            }
        }

        return flags;
    }
}
