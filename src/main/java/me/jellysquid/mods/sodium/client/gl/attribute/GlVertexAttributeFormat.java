package me.jellysquid.mods.sodium.client.gl.attribute;

import org.lwjgl.opengl.GL46C;

/**
 * An enumeration over the supported data types that can be used for vertex attributes.
 */
public record GlVertexAttributeFormat(int typeId, int size) {
    public static final GlVertexAttributeFormat FLOAT = new GlVertexAttributeFormat(GL46C.GL_FLOAT, 4);
    public static final GlVertexAttributeFormat UNSIGNED_SHORT = new GlVertexAttributeFormat(GL46C.GL_UNSIGNED_SHORT, 2);
    public static final GlVertexAttributeFormat UNSIGNED_BYTE = new GlVertexAttributeFormat(GL46C.GL_UNSIGNED_BYTE, 1);
    public static final GlVertexAttributeFormat UNSIGNED_INT = new GlVertexAttributeFormat(GL46C.GL_UNSIGNED_INT, 4);
}
