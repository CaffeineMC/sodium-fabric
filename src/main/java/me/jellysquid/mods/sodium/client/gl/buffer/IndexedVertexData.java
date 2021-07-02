package me.jellysquid.mods.sodium.client.gl.buffer;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * Helper type for tagging the vertex format alongside the raw buffer data.
 */
public class IndexedVertexData {
    public final GlVertexFormat<?> vertexFormat;

    public final NativeBuffer vertexBuffer;
    public final NativeBuffer indexBuffer;

    public IndexedVertexData(GlVertexFormat<?> vertexFormat, NativeBuffer vertexBuffer, NativeBuffer indexBuffer) {
        this.vertexFormat = vertexFormat;
        this.vertexBuffer = vertexBuffer;
        this.indexBuffer = indexBuffer;
    }

    public void delete() {
        this.vertexBuffer.free();
        this.indexBuffer.free();
    }
}
