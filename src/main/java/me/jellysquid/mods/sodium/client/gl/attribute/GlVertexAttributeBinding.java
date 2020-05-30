package me.jellysquid.mods.sodium.client.gl.attribute;

/**
 * An immutable binding between the generic attribute in a given vertex format and shader input.
 */
public class GlVertexAttributeBinding {
    public final int index;
    public final int count;
    public final int format;
    public final int stride;
    public final long pointer;
    public final boolean normalized;

    public <T extends Enum<T>> GlVertexAttributeBinding(int index, GlVertexFormat<T> format, T key) {
        GlVertexAttribute attribute = format.getAttribute(key);

        this.index = index;
        this.count = attribute.getCount();
        this.format = attribute.getFormat();
        this.stride = format.getStride();
        this.pointer = attribute.getPointer();
        this.normalized = attribute.isNormalized();
    }
}
