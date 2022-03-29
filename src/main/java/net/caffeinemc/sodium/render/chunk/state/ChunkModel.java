package net.caffeinemc.sodium.render.chunk.state;

import net.caffeinemc.sodium.render.buffer.VertexRange;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;

public final class ChunkModel {
    private final ChunkRenderPass pass;
    private final VertexRange[] ranges;

    private final int visibilityBits;

    public ChunkModel(ChunkRenderPass pass, VertexRange[] ranges) {
        this.pass = pass;
        this.ranges = ranges;

        this.visibilityBits = calculateVisibilityBits(ranges);
    }

    public ChunkRenderPass getRenderPass() {
        return this.pass;
    }

    public VertexRange[] getModelRanges() {
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
