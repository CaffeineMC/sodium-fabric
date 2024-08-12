package net.caffeinemc.mods.sodium.client.render.chunk.data;

import net.caffeinemc.mods.sodium.client.util.NativeBuffer;

public class BuiltSectionMeshParts {
    private final int[] vertexCounts;
    private final NativeBuffer buffer;

    public BuiltSectionMeshParts(NativeBuffer buffer, int[] vertexCounts) {
        this.vertexCounts = vertexCounts;
        this.buffer = buffer;
    }

    public NativeBuffer getVertexData() {
        return this.buffer;
    }

    public int[] getVertexCounts() {
        return this.vertexCounts;
    }
}
