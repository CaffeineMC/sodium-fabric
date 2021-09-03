package me.jellysquid.mods.sodium.render;

import me.jellysquid.mods.thingl.attribute.GlVertexFormat;
import me.jellysquid.mods.thingl.util.NativeBuffer;

/**
 * Helper type for tagging the vertex format alongside the raw buffer data.
 */
public record IndexedVertexData(GlVertexFormat<?> vertexFormat,
                                NativeBuffer vertexBuffer,
                                NativeBuffer indexBuffer) {
    public void delete() {
        this.vertexBuffer.free();
        this.indexBuffer.free();
    }
}
