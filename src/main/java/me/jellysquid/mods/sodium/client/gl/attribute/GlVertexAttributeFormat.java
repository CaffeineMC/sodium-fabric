package me.jellysquid.mods.sodium.client.gl.attribute;

import org.lwjgl.opengl.GL11;

/**
 * An enumeration over the supported data types that can be used for vertex attributes.
 */
public class GlVertexAttributeFormat {
    public static final GlVertexAttributeFormat FLOAT = new GlVertexAttributeFormat(GL11.GL_FLOAT, 4);
    public static final GlVertexAttributeFormat UNSIGNED_SHORT = new GlVertexAttributeFormat(GL11.GL_UNSIGNED_SHORT, 2);
    public static final GlVertexAttributeFormat UNSIGNED_BYTE = new GlVertexAttributeFormat(GL11.GL_UNSIGNED_BYTE, 1);

    private final int glId;
    private final int size;

    private GlVertexAttributeFormat(int glId, int size) {
        this.glId = glId;
        this.size = size;
    }

    public int getSize() {
        return this.size;
    }

    public int getGlFormat() {
        return this.glId;
    }
}
