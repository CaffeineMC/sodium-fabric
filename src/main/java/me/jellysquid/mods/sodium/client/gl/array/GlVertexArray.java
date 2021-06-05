package me.jellysquid.mods.sodium.client.gl.array;

import me.jellysquid.mods.sodium.client.gl.GlObject;
import me.jellysquid.mods.sodium.client.gl.func.GlFunctions;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;

/**
 * Provides Vertex Array functionality on supported platforms.
 */
public class GlVertexArray extends GlObject {
    public static final int NULL_ARRAY_ID = 0;

    public GlVertexArray(RenderDevice owner) {
        super(owner);

        if (!GlFunctions.isVertexArraySupported()) {
            throw new UnsupportedOperationException("Vertex arrays are unsupported on this platform");
        }

        this.setHandle(GlFunctions.VERTEX_ARRAY.glGenVertexArrays());
    }
}
