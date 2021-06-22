package me.jellysquid.mods.sodium.client.gl.buffer;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;

import java.nio.ByteBuffer;

/**
 * Helper type for tagging the vertex format alongside the raw buffer data.
 */
public class IndexedVertexData {
    public final GlVertexFormat<?> vertexFormat;

    public final ByteBuffer vertexBuffer;
    public final ByteBuffer indexBuffer;

    public IndexedVertexData(GlVertexFormat<?> vertexFormat, ByteBuffer vertexBuffer, ByteBuffer indexBuffer) {
        this.vertexFormat = vertexFormat;
        this.vertexBuffer = vertexBuffer;
        this.indexBuffer = indexBuffer;
    }
}
