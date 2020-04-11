package me.jellysquid.mods.sodium.client.gl.attribute;

public class GlAttributeBinding {
    public final int index;
    public final int count;
    public final int format;
    public final int stride;
    public final long pointer;
    public final boolean normalized;

    public GlAttributeBinding(int index, int count, int format, boolean normalized, int stride, long pointer) {
        this.index = index;
        this.count = count;
        this.format = format;
        this.stride = stride;
        this.pointer = pointer;
        this.normalized = normalized;
    }
}
