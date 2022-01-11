package me.jellysquid.mods.sodium.render.buffer;

import me.jellysquid.mods.sodium.opengl.attribute.VertexFormat;
import me.jellysquid.mods.sodium.util.NativeBuffer;

/**
 * Helper type for tagging the vertex format alongside the raw buffer data.
 */
public record IndexedVertexData(VertexFormat<?> vertexFormat,
                                NativeBuffer vertexBuffer) {
    public void delete() {
        this.vertexBuffer.free();
    }
}
