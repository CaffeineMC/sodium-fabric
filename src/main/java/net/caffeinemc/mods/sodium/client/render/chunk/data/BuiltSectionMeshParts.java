package net.caffeinemc.mods.sodium.client.render.chunk.data;

import net.caffeinemc.mods.sodium.client.gl.util.VertexRange;
import net.caffeinemc.mods.sodium.client.util.NativeBuffer;

public class BuiltSectionMeshParts {
    private final VertexRange[] ranges;
    private final NativeBuffer buffer;

    public BuiltSectionMeshParts(NativeBuffer buffer, VertexRange[] ranges) {
        this.ranges = ranges;
        this.buffer = buffer;
    }

    public NativeBuffer getVertexData() {
        return this.buffer;
    }

    public VertexRange[] getVertexRanges() {
        return this.ranges;
    }
}
