package me.jellysquid.mods.sodium.opengl.attribute;

import org.lwjgl.opengl.GL20C;

/**
 * An enumeration over the supported data types that can be used for vertex attributes.
 */
public record VertexAttributeFormat(int typeId, int size) {
    public static final VertexAttributeFormat FLOAT = new VertexAttributeFormat(GL20C.GL_FLOAT, 4);
    public static final VertexAttributeFormat UNSIGNED_SHORT = new VertexAttributeFormat(GL20C.GL_UNSIGNED_SHORT, 2);
    public static final VertexAttributeFormat UNSIGNED_BYTE = new VertexAttributeFormat(GL20C.GL_UNSIGNED_BYTE, 1);
    public static final VertexAttributeFormat UNSIGNED_INT = new VertexAttributeFormat(GL20C.GL_UNSIGNED_INT, 4);
}
