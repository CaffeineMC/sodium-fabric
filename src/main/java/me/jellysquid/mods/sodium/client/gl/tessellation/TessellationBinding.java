package me.jellysquid.mods.sodium.client.gl.tessellation;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;

public class TessellationBinding {
    private final GlBuffer buffer;
    private final GlVertexAttributeBinding[] bindings;

    public TessellationBinding(GlBuffer buffer, GlVertexAttributeBinding[] bindings) {
        this.buffer = buffer;
        this.bindings = bindings;
    }

    public GlBuffer getBuffer() {
        return this.buffer;
    }

    public GlVertexAttributeBinding[] getAttributeBindings() {
        return this.bindings;
    }
}
