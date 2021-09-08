package me.jellysquid.mods.thingl.tessellation.binding;

import me.jellysquid.mods.thingl.attribute.VertexAttributeBinding;
import me.jellysquid.mods.thingl.attribute.VertexFormat;
import me.jellysquid.mods.thingl.buffer.Buffer;

public final class VertexBufferBinding {
    private final Buffer buffer;
    private final int stride;
    private final VertexAttributeBinding[] attributeBindings;

    public VertexBufferBinding(Buffer buffer, VertexFormat<?> format, VertexAttributeBinding[] attributeBindings) {
        this.buffer = buffer;
        this.stride = format.getStride();
        this.attributeBindings = attributeBindings;
    }

    public Buffer getBuffer() {
        return this.buffer;
    }

    public int getStride() {
        return this.stride;
    }

    public VertexAttributeBinding[] getAttributeBindings() {
        return this.attributeBindings;
    }
}
