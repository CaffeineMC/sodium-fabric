package me.jellysquid.mods.sodium.client.render.chunk.data;

import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;

public class BuiltSectionMeshParts {
    private final VertexRange[] parts;
    private final NativeBuffer buffer;

    public BuiltSectionMeshParts(NativeBuffer buffer, VertexRange[] parts) {
        this.parts = parts;
        this.buffer = buffer;
    }

    public VertexRange[] getParts() {
        return this.parts;
    }

    public NativeBuffer getVertexData() {
        return this.buffer;
    }
}
