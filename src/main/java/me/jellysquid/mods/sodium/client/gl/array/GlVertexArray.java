package me.jellysquid.mods.sodium.client.gl.array;

import me.jellysquid.mods.sodium.client.gl.GlObject;
import me.jellysquid.mods.sodium.client.gl.func.GlFunctions;

/**
 * Provides Vertex Array functionality on supported platforms.
 */
public class GlVertexArray extends GlObject {
    public GlVertexArray() {
        if (!isSupported()) {
            throw new UnsupportedOperationException("Vertex arrays are unsupported on this platform");
        }

        this.setHandle(GlFunctions.VERTEX_ARRAY.glGenVertexArrays());
    }

    public void unbind() {
        GlFunctions.VERTEX_ARRAY.glBindVertexArray(0);
    }

    public void bind() {
        GlFunctions.VERTEX_ARRAY.glBindVertexArray(this.handle());
    }

    public void delete() {
        GlFunctions.VERTEX_ARRAY.glDeleteVertexArrays(this.handle());

        this.invalidateHandle();
    }

    public static boolean isSupported() {
        return GlFunctions.isVertexArraySupported();
    }
}
