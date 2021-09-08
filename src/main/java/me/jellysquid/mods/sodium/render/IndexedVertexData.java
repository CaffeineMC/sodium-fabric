package me.jellysquid.mods.sodium.render;

import me.jellysquid.mods.thingl.attribute.VertexFormat;
import me.jellysquid.mods.thingl.util.NativeBuffer;

/**
 * Helper type for tagging the vertex format alongside the raw buffer data.
 */
public record IndexedVertexData(VertexFormat<?> vertexFormat,
                                NativeBuffer vertexBuffer,
                                NativeBuffer indexBuffer) {
    public void delete() {
        this.vertexBuffer.free();
        this.indexBuffer.free();
    }
}
